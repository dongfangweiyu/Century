package com.ra.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.common.utils.SignUtils;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.factory.ConfigFactory;
import com.ra.dao.repository.business.PayOrderRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.params.CreateOrderResp;
import com.ra.service.bean.params.OrderCreateParams;
import com.ra.service.bean.params.OrderQueryParams;
import com.ra.service.bean.params.OrderQueryResp;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.resp.MerchantUserInfoVo;
import com.ra.service.bean.resp.PayOrderTotalVo;
import com.ra.service.bean.resp.PayOrderVo;
import com.ra.service.business.*;
import com.ra.service.component.PayOrderComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayOrderServiceImpl extends BaseServiceImpl<PayOrderRepository> implements PayOrderService {

    private static final Logger logger= LoggerFactory.getLogger(PayOrderServiceImpl.class);

    @Autowired
    private PayCodeService payCodeService;
    @Autowired
    private PayChannelService payChannelService;
    @Autowired
    private RateService rateService;
    @Autowired
    private PayOrderComponent payOrderComponent;
    @Autowired
    private MerchantInfoService merchantInfoService;
    @Autowired
    private IncomeLogService incomeLogService;

    @Override
    public List<OrderQueryResp> findMerchantQueryOtcOrder(OrderQueryParams params) {
        MerchantUserInfoVo merchantInfo = merchantInfoService.findMerchantInfo(params.getAppId());

        String sign = SignUtils.generateQuerySign(params.getAppId(), params.getTimestamp(), params.getNonceStr(), merchantInfo.getMerchantInfo().getSecret());
        Assert.isTrue(sign.equalsIgnoreCase(params.getSignature()),"签名不正确");

        Map<String,Object> map=new HashMap<>();
        map.put("merchantUserId",merchantInfo.getMerchantInfo().getUserId());
        String sql="select a.* from business_order_pay as a where 1=1 and a.merchantUserId=:merchantUserId ";
        if(!StringUtils.isEmpty(params.getOrderNo())){
            sql+=" and a.orderNo =:orderNo";
            map.put("orderNo",params.getOrderNo());
        }
        if(!StringUtils.isEmpty(params.getOutOrderNo())){
            sql+=" and a.outOrderNo =:outOrderNo";
            map.put("outOrderNo",params.getOutOrderNo());
        }
        return findList(sql,map, OrderQueryResp.class);
    }

    @Override
    public PayOrder findOldCreateOrder(String outOrderNo, long merchantUserId) {
        Map<String,Object> params=new HashMap<>();
        params.put("outOrderNo",outOrderNo);
        params.put("merchantUserId",merchantUserId);
        String sql="select * from business_order_pay where outOrderNo=:outOrderNo and status='PROCESS' and merchantUserId=:merchantUserId order by id asc ";
        return findOne(sql,params,PayOrder.class);
    }


    @Override
    @Transactional
    public CreateOrderResp createOrder(OrderCreateParams params) {

        PayCode byCode = payCodeService.getRepository().findByCode(params.getPayCode());
        Assert.notNull(byCode,"参数payCode：不存在的通道编码");
        Assert.isTrue(byCode.isEnable(),"该通道维护中...错误代码："+byCode.getCode());

        MerchantUserInfoVo merchantInfo = merchantInfoService.findMerchantInfo(params.getAppId());
        logger.info("请求签名："+params.getSignature());
        String sign = SignUtils.generateCreateSign(params.getOutOrderNo(),params.getAmount(),params.getPayCode(),params.getAttach()
                ,params.getAppId(), params.getTimestamp(),params.getNonceStr(),merchantInfo.getMerchantInfo().getSecret());
        logger.info("验证签名:"+sign);
        if (!params.getSignature().equals(sign)) {
            throw new IllegalArgumentException("签名不正确");
        }


        PayOrder payOrder =findOldCreateOrder(params.getOutOrderNo(),merchantInfo.getMerchantInfo().getUserId());
        if(payOrder==null){
            PayChannel payChannel = payChannelService.matchChannel(params.getAmount(),merchantInfo.getMerchantInfo().getId(), byCode.getId());
            Assert.notNull(payChannel,"创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：PAYCHANNEL");

            Rate rate = rateService.getRepository().findRateByMerchantInfoIdAndPayChannelId(merchantInfo.getMerchantInfo().getId(), payChannel.getId());
            Assert.notNull(rate,"创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：RATE");

            payOrder =new PayOrder();
            payOrder.setOrderNo(PayOrder.genaralOrderNo(ConfigFactory.get(ConfigEnum.ORDER_PREFIX)));
            payOrder.setOutOrderNo(params.getOutOrderNo());
            payOrder.setAmount(params.getAmount());
            payOrder.setAttach(params.getAttach());
            payOrder.setMerchantUserId(merchantInfo.getMerchantInfo().getUserId());
            payOrder.setStatus(OrderStatusEnum.PROCESS);
            payOrder.setPayCode(params.getPayCode());
            payOrder.setStatusDesc("新订单生成,等待支付");
            payOrder.setNotifyCount(0);
            payOrder.setNotifyUrl(params.getNotifyUrl());
            payOrder.setReturnUrl(params.getReturnUrl());
            payOrder.setOrderDesc(params.getOrderDesc());
            payOrder.setPayChannelId(payChannel.getId());
            payOrder.setPayChannelInfoJson(JSON.toJSONString(payChannel));
            payOrder.setMerchantAccount(merchantInfo.getUser().getAccount());
            payOrder.setRateInfoJson(JSON.toJSONString(rate));
            payOrder = getRepository().save(payOrder);
        }
        return new CreateOrderResp(payOrder);
    }

    @Override
    @Transactional
    public boolean successOrder(PayOrder payOrder,String statusDesc) {
        statusDesc=StringUtils.isEmpty(statusDesc)?"订单已支付,等待回调":statusDesc;
        int i = getRepository().successOrder(payOrder.getId(), statusDesc);
        if(i>0){
            boolean b=incomeLogService.saveIncomeLog(payOrder);//保存收入
            if(b){
                payChannelService.getRepository().subCreditScore(payOrder.getPayChannelId(), payOrder.getAmount());//扣减信誉分
                payOrderComponent.notifyOrder(payOrder,statusDesc);
                return true;
            }
        }
        throw new IllegalArgumentException("完成订单失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyTaskOrder() {
        long lastTime=System.currentTimeMillis();
        logger.info("************【代收订单异步通知执行 begin】************");
        String hql = "select * from business_order_pay where status='SUCCESS' and completeTime is null  and notifyCount<10 order by createTime asc "; //按照时间升序
        List<PayOrder> list = findList(hql, new HashMap<>(), PayOrder.class);
        logger.info("总共查询到"+list.size()+"条待通知的代收订单...");
        for (PayOrder order : list) {
            int notifyCount = order.getNotifyCount(); //获取通知次数
            long createTime = order.getCreateTime().getTime(); //获取订单的创建时间
            if(notifyCount<10){
                //每隔1分钟回调一次该笔订单
                if(System.currentTimeMillis() - createTime > 60 * 1000 * (notifyCount+1)){
                    payOrderComponent.notifyOrder(order,order.getStatusDesc());
                }
            }
        }
        logger.info("************【代收订单异步通知执行 end】耗时："+(System.currentTimeMillis()-lastTime)+"毫秒************");
    }

    @Override
    @Transactional
    public void closeTaskOrder() {
        getRepository().closeOrderBatch();
    }

    /**
     * 订单列表
     * @param payOrderReq
     * @param pager
     * @return
     */
    @Override
    public Page<PayOrderVo> findListByPayOrder(PayOrderReq payOrderReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select * from business_order_pay o ");
        builder.append(" where 1=1  ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_order_pay o");
        sqlCount.append(" where 1=1  ");
        Map<String, Object> params = new HashMap<>();

        if(payOrderReq.getProxyUserId()!=null){
            builder.append(" and  merchantUserId in (select m.userId from business_merchant_info m where m.proxyUserId=:proxyUserId ) ");
            sqlCount.append(" and  merchantUserId in (select m.userId from business_merchant_info m where m.proxyUserId=:proxyUserId )   ");
            params.put("proxyUserId",payOrderReq.getProxyUserId());
        }

        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            builder.append("  and o.merchantAccount like :merchantAccount");
            sqlCount.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            builder.append(" and o.amount = :amount");
            sqlCount.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            builder.append(" and o.payCode = :payCode");
            sqlCount.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            builder.append(" and o.status = :status");
            sqlCount.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayChannelId())){
            builder.append(" and o.payChannelId = :payChannelId");
            sqlCount.append(" and o.payChannelId = :payChannelId");
            params.put("payChannelId",payOrderReq.getPayChannelId());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            builder.append(" and o.createTime >= :beginTime");
            sqlCount.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            builder.append(" and o.createTime <= :endTime");
            sqlCount.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            builder.append(" and o.orderNo like :orderNo");
            sqlCount.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            builder.append(" and o.outOrderNo like :outOrderNo");
            sqlCount.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by o.id desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<PayOrderVo> list = findList(builder.toString(), params, PayOrderVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public PayOrderTotalVo findByPayOrderTotal(PayOrderReq payOrderReq) {
        StringBuffer sqlCount=new StringBuffer("select \n" +
                " ifnull(sum(if(o.completeTime is not null,o.amount,0)),0) as otcTodayCompleteTotal,\n" +
                " ifnull(sum(if(o.status='SUCCESS',o.amount,0)),0) as otcOrderSuccessTotal,\n" +
                "ifnull(sum(if(o.createTime is not null,o.amount,0)),0) as otcCrateTotal," +
                "ifnull(count(if(o.status='SUCCESS',1,null)),0)as otcSuccessCount from business_order_pay o");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if(payOrderReq.getProxyUserId()!=null){
            sqlCount.append(" and  o.merchantUserId in (select m.userId from business_merchant_info m where m.proxyUserId=:proxyUserId )   ");
            params.put("proxyUserId",payOrderReq.getProxyUserId());
        }

        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            sqlCount.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            sqlCount.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            sqlCount.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            sqlCount.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayChannelId())){
            sqlCount.append(" and o.payChannelId = :payChannelId");
            params.put("payChannelId",payOrderReq.getPayChannelId());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            sqlCount.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            sqlCount.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            sqlCount.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            sqlCount.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }
        return findOne(sqlCount.toString(), params, PayOrderTotalVo.class);
    }

    @Override
    public Page<PayOrderVo> findListByProxyPayOrder(PayOrderReq payOrderReq,long proxyId, Pager pager) {
        StringBuilder builder = new StringBuilder("select o.id,o.orderNo,o.outOrderNo,o.merchantAccount,o.amount,o.payCode,o.createTime,o.completeTime, " +
                " o.closeTime,o.status,o.statusDesc,m.companyName as companyName from business_order_pay o LEFT JOIN business_merchant_info m on o.merchantUserId=m.userId ");
        builder.append(" where 1=1  and m.proxyUserId= :proxyUserId");
        StringBuffer sqlCount=new StringBuffer("select count(1) from business_order_pay o LEFT JOIN business_merchant_info m on o.merchantUserId=m.userId ");
        sqlCount.append(" where 1=1 and m.proxyUserId= :proxyUserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("proxyUserId",proxyId);
        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            builder.append("  and o.merchantAccount like :merchantAccount");
            sqlCount.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            builder.append(" and o.amount = :amount");
            sqlCount.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            builder.append(" and o.payCode = :payCode");
            sqlCount.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            builder.append(" and o.status = :status");
            sqlCount.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayChannelId())){
            builder.append(" and o.payChannelId = :payChannelId");
            sqlCount.append(" and o.payChannelId = :payChannelId");
            params.put("payChannelId",payOrderReq.getPayChannelId());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            builder.append(" and o.createTime >= :beginTime");
            sqlCount.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            builder.append(" and o.createTime <= :endTime");
            sqlCount.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            builder.append(" and o.orderNo like :orderNo");
            sqlCount.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            builder.append(" and o.outOrderNo like :outOrderNo");
            sqlCount.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by o.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<PayOrderVo> list = findList(builder.toString(), params, PayOrderVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public Page<PayOrderVo> findListByMerchantPayOrder(PayOrderReq payOrderReq, long merchangUserId, Pager pager) {
        StringBuilder builder = new StringBuilder("select o.* from business_order_pay o ");
        builder.append(" where 1=1  and o.merchantUserId= :merchangUserId");
        StringBuffer sqlCount=new StringBuffer("select count(1) from business_order_pay o ");
        sqlCount.append(" where 1=1 and o.merchantUserId= :merchangUserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("merchangUserId",merchangUserId);
        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            builder.append("  and o.merchantAccount like :merchantAccount");
            sqlCount.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            builder.append(" and o.amount = :amount");
            sqlCount.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            builder.append(" and o.payCode = :payCode");
            sqlCount.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            builder.append(" and o.status = :status");
            sqlCount.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            builder.append(" and o.createTime >= :beginTime");
            sqlCount.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            builder.append(" and o.createTime <= :endTime");
            sqlCount.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            builder.append(" and o.orderNo like :orderNo");
            sqlCount.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            builder.append(" and o.outOrderNo like :outOrderNo");
            sqlCount.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by o.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<PayOrderVo> list = findList(builder.toString(), params, PayOrderVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findByProxyPayOrderTotal(PayOrderReq payOrderReq, long userId) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer(" select ifnull(sum(o.amount),0) from business_order_pay o LEFT JOIN business_merchant_info m on o.merchantUserId=m.userId");
        sql.append(" where 1=1  and o.status='SUCCESS' and m.proxyUserId= :proxyUserId");
        params.put("proxyUserId",userId);
        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            sql.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            sql.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            sql.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            sql.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            sql.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            sql.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            sql.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            sql.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }
        double sum = findSum(sql.toString(), params);
        return BigDecimal.valueOf(sum);
    }

    @Override
    public BigDecimal findByMerchantPayOrderTotal(PayOrderReq payOrderReq, long userId) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer(" select ifnull(sum(o.amount),0) from business_order_pay o ");
        sql.append(" where 1=1  and o.status='SUCCESS' and o.merchantUserId= :merchantUserId ");
        params.put("merchantUserId",userId);
        if (!StringUtils.isEmpty(payOrderReq.getMerchantAccount())) {
            sql.append("  and o.merchantAccount like :merchantAccount");
            params.put("merchantAccount", '%'+payOrderReq.getMerchantAccount()+'%');
        }
        if(payOrderReq.getAmount()!=null&&payOrderReq.getAmount()>0){
            sql.append(" and o.amount = :amount");
            params.put("amount",payOrderReq.getAmount());
        }
        if(!StringUtils.isEmpty(payOrderReq.getPayCode())){
            sql.append(" and o.payCode = :payCode");
            params.put("payCode",payOrderReq.getPayCode());
        }
        if (!StringUtils.isEmpty(payOrderReq.getStatus())) {
            sql.append(" and o.status = :status");
            params.put("status",payOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(payOrderReq.getBeginTime())) {
            sql.append(" and o.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(payOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(payOrderReq.getEndTime())) {
            sql.append(" and o.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(payOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(payOrderReq.getOrderNo())) {
            sql.append(" and o.orderNo like :orderNo");
            params.put("orderNo",'%'+payOrderReq.getOrderNo()+'%');
        }
        if (!StringUtils.isEmpty(payOrderReq.getOutOrderNo())) {
            sql.append(" and o.outOrderNo like :outOrderNo");
            params.put("outOrderNo",'%'+payOrderReq.getOutOrderNo()+'%');
        }
        double sum = findSum(sql.toString(), params);
        return  BigDecimal.valueOf(sum);
    }
}

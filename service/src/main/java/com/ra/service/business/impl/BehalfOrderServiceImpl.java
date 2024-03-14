package com.ra.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.SignUtils;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.dao.factory.ConfigFactory;
import com.ra.dao.repository.business.BehalfOrderRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.params.*;
import com.ra.service.bean.req.BehalfOrderReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.*;
import com.ra.service.component.BehalfOrderComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehalfOrderServiceImpl extends BaseServiceImpl<BehalfOrderRepository> implements BehalfOrderService {

    private static final Logger logger= LoggerFactory.getLogger(BehalfOrderServiceImpl.class);

    @Autowired
    MerchantInfoService merchantInfoService;
    @Autowired
    BehalfBankCardService behalfBankCardService;
    @Autowired
    WalletService walletService;
    @Autowired
    BehalfOrderComponent behalfOrderComponent;
    @Autowired
    IncomeLogService incomeLogService;
    @Autowired
    BehalfInfoService behalfInfoService;

    @Override
    public List<BehalfOrderQueryResp> findMerchantQueryOtcOrder(OrderQueryParams params) {
        MerchantUserInfoVo merchantInfo = merchantInfoService.findMerchantInfo(params.getAppId());

        String sign = SignUtils.generateQuerySign(params.getAppId(), params.getTimestamp(), params.getNonceStr(), merchantInfo.getMerchantInfo().getSecret());
        Assert.isTrue(sign.equalsIgnoreCase(params.getSignature()),"签名不正确");

        Map<String,Object> map=new HashMap<>();
        map.put("merchantUserId",merchantInfo.getMerchantInfo().getUserId());
        String sql="select a.* from business_behalf_order as a where 1=1  and a.merchantUserId=:merchantUserId ";
        if(!StringUtils.isEmpty(params.getOrderNo())){
            sql+=" and a.orderNo =:orderNo";
            map.put("orderNo",params.getOrderNo());
        }
        if(!StringUtils.isEmpty(params.getOutOrderNo())){
            sql+=" and a.outOrderNo =:outOrderNo";
            map.put("outOrderNo",params.getOutOrderNo());
        }
        return findList(sql,map, BehalfOrderQueryResp.class);
    }

    @Override
    public List<BehalfOrderListQueryResp> findBehalfQueryOtcOrder(OrderQueryParams params) {
        BehalfUserInfoVo behalfUserInfoVo = behalfInfoService.findBehalfInfo(params.getAppId());

        String sign = SignUtils.generateQuerySign(params.getAppId(), params.getTimestamp(), params.getNonceStr(), behalfUserInfoVo.getBehalfInfo().getSecret());
        Assert.isTrue(sign.equalsIgnoreCase(params.getSignature()),"签名不正确");

        Map<String,Object> map=new HashMap<>();
        map.put("behalfUserId",behalfUserInfoVo.getBehalfInfo().getUserId());
        String sql="select a.* from business_behalf_order as a where 1=1  and a.behalfUserId=:behalfUserId and status='PROCESS' ";
        return findList(sql,map, BehalfOrderListQueryResp.class);
    }

    @Override
    public Wallet findMerchantWallet(OrderQueryParams params) {
        MerchantUserInfoVo merchantInfo = merchantInfoService.findMerchantInfo(params.getAppId());
        String sign = SignUtils.generateQuerySign(params.getAppId(), params.getTimestamp(), params.getNonceStr(), merchantInfo.getMerchantInfo().getSecret());
        Assert.isTrue(sign.equalsIgnoreCase(params.getSignature()),"签名不正确");
        Wallet wallet = walletService.findWallet(merchantInfo.getMerchantInfo().getUserId());
        return wallet;
    }

    @Override
    @Transactional
    public BehalfOrder createOrder(HttpServletRequest request, BehalfOrderCreateParams params) {

        MerchantUserInfoVo merchantInfo = merchantInfoService.findMerchantInfo(params.getAppId());
        logger.info("请求签名："+params.getSignature());
        String sign = SignUtils.generateCreateSign(params.getOutOrderNo(),params.getAmount(),null,params.getAttach()
                ,params.getAppId(), params.getTimestamp(),params.getNonceStr(),merchantInfo.getMerchantInfo().getSecret());
        logger.info("验证签名:"+sign);
        if (!params.getSignature().equals(sign)) {
            throw new IllegalArgumentException("签名不正确");
        }


        String bindIp = merchantInfo.getMerchantInfo().getBehalfIp();
        //如果没有绑定IP
        if(!org.springframework.util.StringUtils.isEmpty(bindIp)){  //没有绑定IP则直接跳过
            logger.info(merchantInfo.getMerchantInfo().getCompanyName()+"绑定的代付下单IP："+bindIp);
            String ipAddr = IpUtil.getIpAddr(request);
            logger.info("本次的代付下单IP："+ipAddr);
            if(!bindIp.contains(ipAddr)){//如果IP不匹配则直接报错
                throw new IllegalArgumentException("下单IP不正确");
            }
        }

        //查询有没有重复提交
        BehalfOrder behalfOrder =findOldCreateOrder(params.getOutOrderNo(),merchantInfo.getMerchantInfo().getUserId());
        if(behalfOrder ==null){
            //匹配代付商的银行卡
            BehalfBankCardResp behalfBankCard = behalfBankCardService.matchBehalfBankCard(merchantInfo.getMerchantInfo().getUserId(),false);
            Assert.notNull(behalfBankCard,"创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：BEHALFBANKCARD");

            BigDecimal rateAmount = behalfBankCard.getMerchantRate().multiply(params.getAmount());//手续费金额
            BigDecimal feeAmount=behalfBankCard.getMerchantFee();//收取商户单笔

            BigDecimal totalFee = params.getAmount().add(rateAmount).add(feeAmount);//该订单总需要扣除的费用
            Wallet wallet = walletService.findWallet(merchantInfo.getMerchantInfo().getUserId());
            if(wallet.getMoney().compareTo(totalFee)==-1){
                throw new IllegalArgumentException("通道余额不足");
            }
            int i = walletService.subMoney(merchantInfo.getMerchantInfo().getUserId(), totalFee, WalletLogEnum.MERCHANTBEHALF);
            if(i<=0){
                throw new IllegalArgumentException("通道余额不足");
            }
            //new一个代付订单
            behalfOrder =new BehalfOrder();
            behalfOrder.setOrderNo(BehalfOrder.genaralOrderNo("DF"));
            behalfOrder.setOutOrderNo(params.getOutOrderNo());
            behalfOrder.setAmount(params.getAmount());
            behalfOrder.setRateAmount(rateAmount);
            behalfOrder.setFeeAmount(feeAmount);
            behalfOrder.setMerchantUserId(merchantInfo.getMerchantInfo().getUserId());
            behalfOrder.setStatus(OrderStatusEnum.PROCESS);
            behalfOrder.setStatusDesc("订单创建成功,处理中");
            behalfOrder.setAttach(params.getAttach());
            behalfOrder.setNotifyUrl(params.getNotifyUrl());
            behalfOrder.setBankCardInfoJson(JSON.toJSONString(params.getBankCard()));
            behalfOrder.setBehalfBankCardInfoJson(JSON.toJSONString(behalfBankCard));
            behalfOrder.setMerchantBankNo(params.getBankCard().getBankNo());
            behalfOrder.setBehalfBankNo(behalfBankCard.getBankNo());
            behalfOrder.setBehalfUserId(behalfBankCard.getBehalfUserId());
            behalfOrder = getRepository().save(behalfOrder);
        }
        return behalfOrder;
    }

    @Override
    @Transactional
    public boolean behalfAutomaticConfirmOrder(HttpServletRequest request, BehalfOrderAutomaticParams params) {
        BehalfUserInfoVo behalfUserInfoVo=behalfInfoService.findBehalfInfo(params.getAppId());

        BehalfOrder behalfOrder=getRepository().findByOrderNo(params.getOrderNo());
        Assert.notNull(behalfOrder,"查询不到该代付订单");

        if(behalfOrder.getStatus()!=OrderStatusEnum.PROCESS){
            throw new IllegalArgumentException("该订单不是待处理状态，请核实");
        }
        if(behalfOrder.getAmount().compareTo(params.getAmount())!=0){
            throw new IllegalArgumentException("确认金额与订单金额不符！");
        }
        /* Assert.notNull(behalfOrder.getPaymentVoucher(),"请联系卡商上传凭证，不然无法确认订单");*/
        if(behalfUserInfoVo.getUser().getAgencyType()!= AgencyEnum.BEHALF&&behalfOrder.getBehalfUserId()==behalfUserInfoVo.getUser().getId()){
            throw new IllegalArgumentException("只有该代付订单的卡商才有权限自动审核该订单!");
        }

        String bindIp = behalfUserInfoVo.getBehalfInfo().getBehalfIp();
        //如果没有绑定IP
        if(!org.springframework.util.StringUtils.isEmpty(bindIp)){  //没有绑定IP则直接跳过
            logger.info(behalfUserInfoVo.getBehalfInfo().getCompanyName()+"绑定的代付自动确认IP："+bindIp);
            String ipAddr = IpUtil.getIpAddr(request);
            logger.info("本次的代付订单自动确认请求IP地址为："+ipAddr);
            if(!bindIp.contains(ipAddr)){//如果IP不匹配则直接报错
                throw new IllegalArgumentException("自动化请求IP不正确");
            }
        }
        logger.info("请求签名："+params.getSignature());
        String sign = SignUtils.generateAutomaticConfirmSign(params.getOrderNo(),params.getAmount()
                ,params.getAppId(), params.getTimestamp(),params.getNonceStr(),behalfUserInfoVo.getBehalfInfo().getSecret());
        logger.info("验证签名:"+sign);
        if (!params.getSignature().equals(sign)) {
            throw new IllegalArgumentException("签名不正确");
        }
        boolean i;
        if("confirm".equals(params.getAuditRequest())){
            i =this.compeleteBehalfOrder(behalfOrder,behalfUserInfoVo.getUser());
        }else if("reject".equals(params.getAuditRequest())){
            String remark=behalfUserInfoVo.getUser().getAccount()+"因"+params.getRemark()+"取消订单！";
            behalfOrder.setRemark(params.getRemark());
            i = this.cancelBehalfOrder(behalfOrder,remark);
        }else{
            throw new IllegalArgumentException("审核请求不属于该星球！");
        }
        return i;
    }

    @Override
    public BehalfOrder findOldCreateOrder(String outOrderNo, long merchantUserId) {
        Map<String,Object> params=new HashMap<>();
        params.put("outOrderNo",outOrderNo);
        params.put("merchantUserId",merchantUserId);
        String sql="select * from business_behalf_order where outOrderNo=:outOrderNo and status='PROCESS' and merchantUserId=:merchantUserId order by id asc ";
        return findOne(sql,params, BehalfOrder.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyTaskOrder() {
        long lastTime=System.currentTimeMillis();
        logger.info("************【代付订单异步通知执行 begin】************");
        String hql = "select * from business_behalf_order where status!='PROCESS' and completeTime is null  and notifyCount<10 order by createTime asc "; //按照时间升序
        List<BehalfOrder> list = findList(hql, new HashMap<>(), BehalfOrder.class);
        logger.info("总共查询到"+list.size()+"条待通知的代付订单...");
        for (BehalfOrder order : list) {
            int notifyCount = order.getNotifyCount(); //获取通知次数
            long createTime = order.getCreateTime().getTime(); //获取订单的创建时间
            if(notifyCount<10){
                //每隔1分钟回调一次该笔订单
                if(System.currentTimeMillis() - createTime > 60 * 1000 * (notifyCount+1)){
                    behalfOrderComponent.notifyOrder(order,order.getStatus().toString(),order.getStatusDesc());
                }
            }
        }
        logger.info("************【代付订单异步通知执行 end】耗时："+(System.currentTimeMillis()-lastTime)+"毫秒************");
    }

    @Override
    public Page<BehalfOrderVo> findBehalfOrderList(BehalfOrderReq behalfOrderReq, Long merchantUserId, Long behalfUserId, Pager pager) {
        StringBuilder builder = new StringBuilder("select w.*,u.account as withdrawAccount,b.account as behalfAccount from business_behalf_order w LEFT JOIN security_user u on w.merchantUserId=u.id LEFT JOIN security_user b on w.behalfUserId=b.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_behalf_order w LEFT JOIN security_user u on w.merchantUserId=u.id LEFT JOIN security_user b on w.behalfUserId=b.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        //商户查看提现订单列表时传 总后台看传Null
        if (!StringUtils.isEmpty(merchantUserId)) {
            builder.append("  and w.merchantUserId= :merchantUserId");
            sqlCount.append("  and w.merchantUserId= :merchantUserId ");
            params.put("merchantUserId",merchantUserId);
        }

          //卡商查看自己代付的提现订单时传 总后台看传null
        if (!StringUtils.isEmpty(behalfUserId)) {
            builder.append("  and w.behalfUserId= :behalfUserId");
            sqlCount.append("  and w.behalfUserId= :behalfUserId ");
            params.put("behalfUserId",behalfUserId);
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getStatus())) {
            builder.append("  and w.status = :status");
            sqlCount.append("  and w.status = :status");
            params.put("status",behalfOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBeginTime())) {
            builder.append(" and w.createTime >= :beginTime");
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(behalfOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getEndTime())) {
            builder.append(" and w.createTime <= :endTime");
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(behalfOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBeginSuccessTime())) {
            builder.append(" and w.successTime >= :beginSuccessTime");
            sqlCount.append(" and w.successTime >= :beginSuccessTime");
            params.put("beginSuccessTime", DateUtil.stringToDate(behalfOrderReq.getBeginSuccessTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getEndSuccessTime())) {
            builder.append(" and w.successTime <= :endSuccessTime");
            sqlCount.append(" and w.successTime <= :endSuccessTime");
            params.put("endSuccessTime",DateUtil.stringToDate(behalfOrderReq.getEndSuccessTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getOrderNo())) { //系统提现订单号
            builder.append("  and w.orderNo = :orderNo");
            sqlCount.append("  and w.orderNo = :orderNo");
            params.put("orderNo",behalfOrderReq.getOrderNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getMerchantBankNo())) { //收款客户银行卡号
            builder.append("  and w.merchantBankNo = :merchantBankNo");
            sqlCount.append("  and w.merchantBankNo = :merchantBankNo");
            params.put("merchantBankNo",behalfOrderReq.getMerchantBankNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBehalfBankNo())) { //收款客户银行卡号
            builder.append("  and w.behalfBankNo = :behalfBankNo");
            sqlCount.append("  and w.behalfBankNo = :behalfBankNo");
            params.put("behalfBankNo",behalfOrderReq.getBehalfBankNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getOutOrderNo())) {
            builder.append("  and w.outOrderNo = :outOrderNo");
            sqlCount.append("  and w.outOrderNo = :outOrderNo");
            params.put("outOrderNo",behalfOrderReq.getOutOrderNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getWithdrawAccount())) { //提现的商户账号，供总后台看
            builder.append("  and u.account = :withdrawAccount");
            sqlCount.append("  and u.account = :withdrawAccount");
            params.put("withdrawAccount",behalfOrderReq.getWithdrawAccount());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBehalfAccount())) { //卡商的商户账号，供总后台看
            builder.append("  and b.account = :behalfAccount");
            sqlCount.append("  and b.account = :behalfAccount");
            params.put("behalfAccount",behalfOrderReq.getBehalfAccount());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getDealAccount())) {
            builder.append("  and w.dealAccount = :dealAccount");
            sqlCount.append("  and w.dealAccount = :dealAccount");
            params.put("dealAccount",behalfOrderReq.getDealAccount());
        }
        if(behalfOrderReq.getLastId()!=null){
            builder.append("  and w.id > :lastId");
            sqlCount.append("  and w.id > :lastId");
            params.put("lastId",behalfOrderReq.getLastId());
        }
        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by w.id desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<BehalfOrderVo> list = findList(builder.toString(), params, BehalfOrderVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    @Transactional
    public boolean compeleteBehalfOrder(BehalfOrder behalfOrder, User user) {
        String remark;
        if(user.getAgencyType()== AgencyEnum.BEHALF){
             remark="卡商:"+user.getAccount()+"确认下发,已转账";
        }else{
            remark="管理员:"+user.getAccount()+"确认下发,已转账";
        }
        int i =getRepository().successOrder(behalfOrder.getId(),remark,user.getAccount());
        if(i>0){
            boolean b=incomeLogService.saveBehalfIncomeLog(behalfOrder);
            if(b){
                behalfOrderComponent.notifyOrder(behalfOrder,OrderStatusEnum.SUCCESS.toString(),user.getAccount()+"确认订单，开始回调");
                return true;
            }
            return  false;
        }
        throw new IllegalArgumentException("操作失败");
    }

    @Override
    @Transactional
    public boolean cancelBehalfOrder(BehalfOrder behalfOrder, String statusDesc) {
        int i = getRepository().cannelBehalfOrder(behalfOrder.getId(),statusDesc,behalfOrder.getRemark());//将该订单状态改成失败，即取消
        if(i>0){//状态取消成功后，把取消的金额返回到账户上
            BigDecimal totalFee = behalfOrder.getAmount().add(behalfOrder.getRateAmount()).add(behalfOrder.getFeeAmount());//取消该订单把创建订单扣除的费用返回到该账户中去
            int j=walletService.addMoney(behalfOrder.getMerchantUserId(),totalFee,WalletLogEnum.MERCHANTBEHALFCANCEL);
            if(j>0){
                behalfOrderComponent.notifyOrder(behalfOrder,OrderStatusEnum.FAIL.toString(),statusDesc);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean confirmOrder(long orderId, String paymentVoucher, String remark) {
        BehalfOrder order = getRepository().findBehalfOrderById(orderId);
        Assert.notNull(order,"订单不存在");
        if(order.getStatus()!= OrderStatusEnum.PROCESS){
            throw new IllegalArgumentException("只有处理中状态才能确认付款");
        }
        int i = getRepository().confirmBehalfOrder(orderId,paymentVoucher,remark);
        if(i>0){
            return true;
        }
        return false;
    }

    @Override
    public BehalfOrderTotalVo findByBehalfOrderTotal(BehalfOrderReq behalfOrderReq, Long merchantUserId, Long behalfUserId) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer(" select ifnull(sum(if(w.status ='SUCCESS',w.amount,0)),0) as behalfOrderSuccessTotal" +
                ",ifnull(sum(w.amount),0) as behalfOrderMoneyTotal,ifnull(count(w.id),0) as totalBehalfOrderCount," +
                "  count(IF(w.status='SUCCESS',true,null)) as behalfOrderSuccessCount from business_behalf_order w LEFT JOIN security_user u on w.merchantUserId=u.id LEFT JOIN security_user b on w.behalfUserId=b.id ");
        sql.append(" where 1=1   ");
        //商户查看提现订单列表时传 总后台看传Null
        if (!StringUtils.isEmpty(merchantUserId)) {
            sql.append("  and w.merchantUserId= :merchantUserId");
            params.put("merchantUserId",merchantUserId);
        }

        //卡商查看自己代付的提现订单时传 总后台看传null
        if (!StringUtils.isEmpty(behalfUserId)) {
            sql.append("  and w.behalfUserId= :behalfUserId");
            params.put("behalfUserId",behalfUserId);
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getStatus())) {
            sql.append("  and w.status = :status");
            params.put("status",behalfOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBeginTime())) {
            sql.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(behalfOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getEndTime())) {
            sql.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(behalfOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBeginSuccessTime())) {
            sql.append(" and w.successTime >= :beginSuccessTime");
            params.put("beginSuccessTime", DateUtil.stringToDate(behalfOrderReq.getBeginSuccessTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getEndSuccessTime())) {
            sql.append(" and w.successTime <= :endSuccessTime");
            params.put("endSuccessTime",DateUtil.stringToDate(behalfOrderReq.getEndSuccessTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getOrderNo())) { //系统提现订单号
            sql.append("  and w.orderNo = :orderNo");
            params.put("orderNo",behalfOrderReq.getOrderNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getOutOrderNo())) {
            sql.append("  and w.outOrderNo = :outOrderNo");
            params.put("outOrderNo",behalfOrderReq.getOutOrderNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBehalfAccount())) { //卡商的商户账号，供总后台看
            sql.append("  and b.account = :behalfAccount");
            params.put("behalfAccount",behalfOrderReq.getBehalfAccount());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getWithdrawAccount())) { //提现的商户账号，供总后台看
            sql.append("  and u.account = :withdrawAccount");
            params.put("withdrawAccount",behalfOrderReq.getWithdrawAccount());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getMerchantBankNo())) { //收款客户银行卡号
            sql.append("  and w.merchantBankNo = :merchantBankNo");
            params.put("merchantBankNo",behalfOrderReq.getMerchantBankNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getBehalfBankNo())) { //收款客户银行卡号
            sql.append("  and w.behalfBankNo = :behalfBankNo");
            params.put("behalfBankNo",behalfOrderReq.getBehalfBankNo());
        }
        if (!StringUtils.isEmpty(behalfOrderReq.getDealAccount())) {
            sql.append("  and w.dealAccount = :dealAccount");
            params.put("dealAccount",behalfOrderReq.getDealAccount());
        }
        return findOne(sql.toString(), params, BehalfOrderTotalVo.class);
    }
}

package com.ra.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.ra.common.bean.ExtraData;
import com.ra.common.domain.ExtraDataInterface;
import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.Enum.IncomeEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.repository.business.IncomeLogRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.bean.resp.IncomeTotalVo;
import com.ra.service.bean.resp.ProxyIncomeLogVo;
import com.ra.service.business.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncomeLogServiceImpl extends BaseServiceImpl<IncomeLogRepository> implements IncomeLogService {

    @Autowired
    private WalletService walletService;
    @Autowired
    private ConfigPayInterfaceService configPayInterfaceService;
    @Autowired
    private MerchantInfoService merchantInfoService;
    @Autowired
    private BehalfInfoService behalfInfoService;
    @Autowired
    private BehalfBankCardGroupRelationService behalfBankCardGroupRelationService;
    @Autowired
    private BehalfBankCardService behalfBankCardService;

    @Override
    @Transactional
    public boolean saveIncomeLog(PayOrder order) {

        MerchantInfo merchantInfo = merchantInfoService.findByUserId(order.getMerchantUserId());
        Rate rate = JSON.parseObject(order.getRateInfoJson(),Rate.class);
        PayChannel payChannel = JSON.parseObject(order.getPayChannelInfoJson(),PayChannel.class);

        if(merchantInfo.getProxyUserId()==null||merchantInfo.getProxyUserId()<=0){
            //如果不存在代理，就在计算的时候把代理费率设置成商户费率一样的，这样就会计算出代理费用0元
            rate.setProxyRate(rate.getMerchantRate());
        }
        BigDecimal merchantRateMoney=order.getAmount().multiply(rate.getMerchantRate()).setScale(5,BigDecimal.ROUND_UP);//商户费率金额
        BigDecimal pRateMoney=order.getAmount().multiply(rate.getProxyRate().subtract(payChannel.getRate())).setScale(5,BigDecimal.ROUND_UP);//平台收入金额
        BigDecimal proxyRateMoney=order.getAmount().multiply(rate.getMerchantRate().subtract(rate.getProxyRate())).setScale(5,BigDecimal.ROUND_UP);//代理收入金额
        BigDecimal channelMoney=order.getAmount().multiply(payChannel.getRate()).setScale(5,BigDecimal.ROUND_UP);//通道扣除费率金额


        //给商户加钱
        int i = walletService.addMoney(order.getMerchantUserId(), order.getAmount().subtract(merchantRateMoney), WalletLogEnum.MERCHANT);
        if(i>0){
            //保存收入记录
            IncomeLog log=new IncomeLog();
            log.setIncomeMoney(order.getAmount().subtract(merchantRateMoney));
            log.setIncomeUserId(merchantInfo.getUserId());
            log.setFromOrderNo(order.getOrderNo());
            log.setFromOrderAmount(order.getAmount());
            log.setIncomeEnum(IncomeEnum.PAYINCOME);
            log.setRate(rate.getMerchantRate());
            log.setDescription("订单成交了,扣除费率后收入了"+order.getAmount().subtract(merchantRateMoney)+"块钱");
            getRepository().save(log);
        }

        //给代理加钱
        if(merchantInfo.getProxyUserId()!=null&&merchantInfo.getProxyUserId()>0){
            i = walletService.addMoney(merchantInfo.getProxyUserId(), proxyRateMoney, WalletLogEnum.PROXY);
            if(i>0){
                //保存收入记录
                IncomeLog log=new IncomeLog();
                log.setIncomeMoney(proxyRateMoney);
                log.setIncomeUserId(merchantInfo.getProxyUserId());
                log.setFromOrderNo(order.getOrderNo());
                log.setFromOrderAmount(order.getAmount());
                log.setRate(rate.getMerchantRate().subtract(rate.getProxyRate()));
                log.setIncomeEnum(IncomeEnum.PAYINCOME);
                log.setDescription("我的商户"+merchantInfo.getCompanyName()+"的订单成交收入了"+proxyRateMoney+"块钱");
                getRepository().save(log);
            }
        }

        //给通道加钱
        configPayInterfaceService.addMoney(payChannel.getConfigPayInterfaceId(),order.getAmount().subtract(channelMoney),"订单："+order.getOrderNo()+"，支付成功,通道费率"+(payChannel.getRate().multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP))+"%,余额增加。");


        //给卡商加钱
        if(!StringUtils.isEmpty(order.getExtraData())){
            ExtraData extraData=JSON.parseObject(order.getExtraData(),ExtraData.class);
            ExtraDataInterface extraDataInterface = extraData.newInstance();
            if(extraDataInterface==null){
                throw new IllegalArgumentException("class not found.");
            }
            extraDataInterface.invoke(order);
        }


        //平台保存收入记录
        IncomeLog log=new IncomeLog();
        log.setIncomeMoney(pRateMoney);
        log.setIncomeUserId(null);
        log.setFromOrderNo(order.getOrderNo());
        log.setFromOrderAmount(order.getAmount());
        log.setRate(rate.getProxyRate().subtract(payChannel.getRate()));
        log.setIncomeEnum(IncomeEnum.PAYINCOME);
        log.setDescription("本通道代理费率"+(rate.getMerchantRate().subtract(rate.getProxyRate())).multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP)+"%第三方通道费率收取了"+payChannel.getRate().multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP)+"%后的纯利润");
        getRepository().save(log);

        return true;
    }

    @Override
    public Page<ProxyIncomeLogVo> findListIncomeLog(ProxyIncomeLogReq proxyIncomeLogReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select l.id,l.fromOrderNo as orderNo,l.fromOrderAmount as amount,l.rate,l.incomeMoney,l.createTime,l.description,l.incomeEnum as incomeType from business_income_log l ");

        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_income_log l ");

        Map<String, Object> params = new HashMap<>();
        if(proxyIncomeLogReq.getIncomeUserId()==null){
            builder.append(" where 1=1 and l.incomeUserId is null ");
            sqlCount.append(" where 1=1 and  l.incomeUserId is null ");
        }else{
            builder.append(" where 1=1 and l.incomeUserId=:proxyUserId ");
            sqlCount.append(" where 1=1 and  l.incomeUserId=:proxyUserId ");
            params.put("proxyUserId",proxyIncomeLogReq.getIncomeUserId());
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getBeginTime())) {
            builder.append(" and l.createTime >= :beginTime");
            sqlCount.append(" and l.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(proxyIncomeLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getIncomeType())) {
            builder.append(" and l.incomeEnum = :incomeEnum");
            sqlCount.append(" and l.incomeEnum = :incomeEnum");
            params.put("incomeEnum",proxyIncomeLogReq.getIncomeType());
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getEndTime())) {
            builder.append(" and l.createTime <= :endTime");
            sqlCount.append(" and l.createTime <= :endTime");
            params.put("endTime", DateUtil.stringToDate(proxyIncomeLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by l.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<ProxyIncomeLogVo> list = findList(builder.toString(), params, ProxyIncomeLogVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findByIncomeLogTotal(ProxyIncomeLogReq proxyIncomeLogReq) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer("select ifnull(sum(l.incomeMoney),0) from business_income_log l ");

        if(proxyIncomeLogReq.getIncomeUserId()==null){
            sql.append(" where 1=1 and l.incomeUserId is null ");
        }else{
            sql.append(" where 1=1 and l.incomeUserId=:proxyUserId");
            params.put("proxyUserId",proxyIncomeLogReq.getIncomeUserId());
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getBeginTime())) {
            sql.append(" and l.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(proxyIncomeLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getEndTime())) {
            sql.append(" and l.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(proxyIncomeLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getIncomeType())) {
            sql.append(" and l.incomeEnum = :incomeEnum");
            params.put("incomeEnum",proxyIncomeLogReq.getIncomeType());
        }
        double sum = findSum(sql.toString(), params);
        return BigDecimal.valueOf(sum);
    }

    @Override
    @Transactional
    public boolean saveBehalfIncomeLog(BehalfOrder behalfOrder) {
        MerchantInfo merchantInfo = merchantInfoService.findByUserId(behalfOrder.getMerchantUserId());//商户信息

        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(behalfOrder.getBehalfUserId());//卡商信息

        BehalfBankCard behalfBankCard = JSON.parseObject(behalfOrder.getBehalfBankCardInfoJson(),BehalfBankCard.class);//代付的付款卡组信息

        BehalfBankCardGroupRelation rate=behalfBankCardGroupRelationService.findBehalfBankCardGroupRelationInfo(behalfOrder.getMerchantUserId(),behalfBankCard.getBehalfGroupId());//获取到该付款卡组的费率信息

        if(merchantInfo.getProxyUserId()==null||merchantInfo.getProxyUserId()<=0){
            //如果不存在代理，就在计算的时候把代理费率设置成商户费率一样的，这样就会计算出代理费用0元
            rate.setProxyRate(rate.getMerchantRate());
            rate.setProxyFee(rate.getMerchantFee());
        }

        BigDecimal pRateMoney=behalfOrder.getAmount().multiply(rate.getProxyRate().subtract(behalfInfo.getBehalfRate())).setScale(5,BigDecimal.ROUND_UP);//平台收入费率金额
        BigDecimal pFeeMoney=rate.getProxyFee().subtract(behalfInfo.getBehalfFee());//平台收取的单笔额外收入
        BigDecimal proxyRateMoney=behalfOrder.getAmount().multiply(rate.getMerchantRate().subtract(rate.getProxyRate())).setScale(5,BigDecimal.ROUND_UP);//代理收入金额
        BigDecimal proxyFeeMoney=rate.getMerchantFee().subtract(rate.getProxyFee());//代理的单笔额外收入
        BigDecimal behalfRateMoney=behalfOrder.getAmount().multiply(behalfInfo.getBehalfRate()).setScale(5,BigDecimal.ROUND_UP);//代付卡商扣除费率金额
        BigDecimal behalfFeeMoney=behalfInfo.getBehalfFee();//卡商收取每笔代付的单笔金额


        BigDecimal profit=pRateMoney.add(pFeeMoney).add(proxyRateMoney).add(proxyFeeMoney);//每笔成交从卡商拿取的利润
        //给卡商扣钱
        int i=walletService.subMoney(behalfOrder.getBehalfUserId(), behalfOrder.getAmount().add(behalfRateMoney).add(behalfFeeMoney), WalletLogEnum.MERCHANTBEHALF);
        int j= behalfBankCardService.subMoney(behalfBankCard.getId(),behalfOrder.getAmount(),BehalfWalletLogEnum.COMEOUT);//扣除匹配的银行卡余额，方便卡商统计
        boolean k=behalfInfoService.addProfit(profit,behalfOrder.getBehalfUserId(),WalletLogEnum.BEHALFPROFIT);//卡商那所剩余利润（代理+平台）
        if(i>0&&j>0&&k){
            //保存收入记录
            IncomeLog log=new IncomeLog();
            log.setIncomeMoney(behalfRateMoney.add(behalfFeeMoney));
            log.setIncomeUserId(behalfOrder.getBehalfUserId());
            log.setFromOrderNo(behalfOrder.getOrderNo());
            log.setFromOrderAmount(behalfOrder.getAmount());
            log.setRate(behalfInfo.getBehalfRate());
            log.setIncomeEnum(IncomeEnum.BEHALFINCOME);
            log.setDescription("订单成交,费率收入"+behalfRateMoney+"块钱,"+"单笔收入"+behalfFeeMoney+"块钱");
            getRepository().save(log);
        }else{
            throw new IllegalArgumentException("卡商余额不足");
        }

        //给代理加钱
        if(merchantInfo.getProxyUserId()!=null&&merchantInfo.getProxyUserId()>0&&(proxyRateMoney.add(proxyFeeMoney)).compareTo(BigDecimal.ZERO)==1){
            i = walletService.addMoney(merchantInfo.getProxyUserId(), proxyRateMoney.add(proxyFeeMoney), WalletLogEnum.PROXYBEHALF);
            if(i>0){
                //保存收入记录
                IncomeLog log=new IncomeLog();
                log.setIncomeMoney(proxyRateMoney.add(proxyFeeMoney));
                log.setIncomeUserId(merchantInfo.getProxyUserId());
                log.setFromOrderNo(behalfOrder.getOrderNo());
                log.setFromOrderAmount(behalfOrder.getAmount());
                log.setRate(rate.getMerchantRate().subtract(rate.getProxyRate()));
                log.setIncomeEnum(IncomeEnum.BEHALFINCOME);
                log.setDescription("我的商户"+merchantInfo.getCompanyName()+"的代付订单成交收入了费率"+proxyRateMoney+"块钱,"+"单笔收入"+proxyFeeMoney+"块钱");
                getRepository().save(log);
            }
        }

        //平台保存收入记录
        IncomeLog log=new IncomeLog();
        log.setIncomeMoney(pRateMoney.add(pFeeMoney));
        log.setIncomeUserId(null);
        log.setFromOrderNo(behalfOrder.getOrderNo());
        log.setFromOrderAmount(behalfOrder.getAmount());
        log.setRate(rate.getProxyRate().subtract(behalfInfo.getBehalfRate()));
        log.setIncomeEnum(IncomeEnum.BEHALFINCOME);
        log.setDescription("本卡组代理费率"+(rate.getMerchantRate().subtract(rate.getProxyRate())).multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP)+"%加"+rate.getProxyFee()+"代理固定单笔,第三方卡商费率收取了"+behalfInfo.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP)+"%加"+behalfInfo.getBehalfFee()+"固定单笔后的纯利润");
        getRepository().save(log);
        return true;
    }

    @Override
    public IncomeTotalVo findByIncomeTypeTotal(ProxyIncomeLogReq proxyIncomeLogReq) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer("select ifnull(sum(if(l.incomeEnum ='PAYINCOME',l.incomeMoney,0)),0) as payIncomeTotal," +
                "ifnull(sum(if(l.incomeEnum ='BEHALFINCOME',l.incomeMoney,0)),0) as behalfIncomeTotal  from business_income_log l ");

        if(proxyIncomeLogReq.getIncomeUserId()==null){
            sql.append(" where 1=1 and l.incomeUserId is null ");
        }else{
            sql.append(" where 1=1 and l.incomeUserId=:proxyUserId");
            params.put("proxyUserId",proxyIncomeLogReq.getIncomeUserId());
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getBeginTime())) {
            sql.append(" and l.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(proxyIncomeLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getEndTime())) {
            sql.append(" and l.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(proxyIncomeLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyIncomeLogReq.getIncomeType())) {
            sql.append(" and l.incomeEnum = :incomeEnum");
            params.put("incomeEnum",proxyIncomeLogReq.getIncomeType());
        }
        return findOne(sql.toString(), params, IncomeTotalVo.class);
    }
}

package com.ra.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.BehalfOrder;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.business.MerchantInfoRepository;
import com.ra.dao.repository.security.UserRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.params.BehalfOrderCreateBankCardParams;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.req.OpenAccountReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.BehalfBankCardService;
import com.ra.service.business.BehalfOrderService;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ouyang
 * @time 2019-01-24
 * @description
 */
@Service
public class MerchantInfoServiceImpl extends BaseServiceImpl<MerchantInfoRepository> implements MerchantInfoService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    BehalfBankCardService behalfBankCardService;
    @Autowired
    WalletService walletService;
    @Autowired
    BehalfOrderService behalfOrderService;

    @Override
    public MerchantUserInfoVo findMerchantInfo(String appId) {
        MerchantInfo merchantInfo = getRepository().findByAppId(appId);
        Assert.notNull(merchantInfo,"无效的APPID");
        Assert.isTrue(merchantInfo.getApplyStatus()==ApplyMerchantEnum.PASS,"商户权限不足,请联系客服");
        User user = userRepository.findUserById(merchantInfo.getUserId());
        Assert.isTrue(user.getStatus()==0,"商户权限不足,请联系客服");

        return new MerchantUserInfoVo(merchantInfo,user);
    }


    @Override
    public Page<MerchantInfoVo> findListMerchantInfo(MerchantQueryReq merchantQueryReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select m.userId as userId,u.account as merchantAccount,m.companyName as companyName,w.money as walletMoney," +
                "ifnull(p.account,'平台自招') as proxyAccount,u.createTime as createTime,u.`status` as status,u.lastLoginTime,u.loginIp,m.proxyUserId as proxyUserId from business_merchant_info m \n" +
                "LEFT JOIN security_user u on m.userId=u.id \n" +
                "LEFT JOIN security_user p on m.proxyUserId=p.id LEFT JOIN business_wallet w on m.userId=w.userId ");
        builder.append("  where 1=1  and u.agencyType='MERCHANT' and m.applyStatus='PASS' ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_merchant_info m \n" +
                "LEFT JOIN security_user u on m.userId=u.id \n" +
                "LEFT JOIN security_user p on m.proxyUserId=p.id LEFT JOIN business_wallet w on m.userId=w.userId ");
        sqlCount.append(" where 1=1  and u.agencyType='MERCHANT' and m.applyStatus='PASS' ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(merchantQueryReq.getQueryParam())) {
            builder.append("  and u.account like :account");
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+merchantQueryReq.getQueryParam()+'%');
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getProxyAccount())) {
            builder.append("  and p.account like :proxyAccount");
            sqlCount.append("  and p.account like :proxyAccount");
            params.put("proxyAccount", '%'+merchantQueryReq.getProxyAccount()+'%');
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getStatus())) {
            builder.append("  and u.status = :status");
            sqlCount.append("  and u.status = :status");
            params.put("status",merchantQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getBeginTime())) {
            builder.append(" and u.createTime >= :beginTime");
            sqlCount.append(" and u.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(merchantQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getEndTime())) {
            builder.append(" and u.createTime <= :endTime");
            sqlCount.append(" and u.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(merchantQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by u.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<MerchantInfoVo> list = findList(builder.toString(), params, MerchantInfoVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public MerchantInfoTotalVo findMerchantTotal(MerchantQueryReq merchantQueryReq) {
        StringBuffer sqlCount=new StringBuffer(" select ifnull(sum(w.money),0) as totalBalance from business_merchant_info m \n" +
                "LEFT JOIN security_user u on m.userId=u.id \n" +
                "LEFT JOIN security_user p on m.proxyUserId=p.id LEFT JOIN business_wallet w on m.userId=w.userId ");
        sqlCount.append(" where 1=1  and u.agencyType='MERCHANT' and m.applyStatus='PASS' ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(merchantQueryReq.getQueryParam())) {
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+merchantQueryReq.getQueryParam()+'%');
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getProxyAccount())) {
            sqlCount.append("  and p.account like :proxyAccount");
            params.put("proxyAccount", '%'+merchantQueryReq.getProxyAccount()+'%');
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getStatus())) {
            sqlCount.append("  and u.status = :status");
            params.put("status",merchantQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getBeginTime())) {
            sqlCount.append(" and u.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(merchantQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(merchantQueryReq.getEndTime())) {
            sqlCount.append(" and u.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(merchantQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        return findOne(sqlCount.toString(),params,MerchantInfoTotalVo.class);
    }

    @Override
    public Page<MerchantInfoVo> findListProxyMerchantInfo(MerchantQueryReq proxyQueryReq, long proxyUserId, Pager pager) {
        StringBuilder builder = new StringBuilder("select m.userId as userId,u.account as merchantAccount,m.companyName as companyName," +
                "u.createTime as createTime,u.`status` as status,w.money as walletMoney from business_merchant_info m \n" +
                "LEFT JOIN security_user u on m.userId=u.id LEFT JOIN business_wallet w on w.userId=m.userId ");
        builder.append("  where 1=1 and m.applyStatus='PASS' and m.proxyUserId=:proxyUserId");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_merchant_info m \n" +
                "LEFT JOIN security_user u on m.userId=u.id ");
        sqlCount.append(" where 1=1 and m.applyStatus='PASS' and m.proxyUserId=:proxyUserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("proxyUserId",proxyUserId);
        if (!StringUtils.isEmpty(proxyQueryReq.getQueryParam())) {
            builder.append("  and u.account like :account");
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+proxyQueryReq.getQueryParam()+'%');
        }
        if (!StringUtils.isEmpty(proxyQueryReq.getStatus())) {
            builder.append("  and u.status = :status");
            sqlCount.append("  and u.status = :status");
            params.put("status",proxyQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(proxyQueryReq.getBeginTime())) {
            builder.append(" and u.createTime >= :beginTime");
            sqlCount.append(" and u.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(proxyQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(proxyQueryReq.getEndTime())) {
            builder.append(" and u.createTime <= :endTime");
            sqlCount.append(" and u.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(proxyQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by u.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<MerchantInfoVo> list = findList(builder.toString(), params, MerchantInfoVo.class);
        return new PagerImpl(list,pager.of(),count);
    }


    /**
     * 根据商户userId查询商户信息
     * @param userId
     * @return
     */
    @Override
    public MerchantInfo findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Page<MerchantInfo> findListByApplyMerchant(Long proxyId,String applyStatua, String beginTime, String endTime, String companyName, Pager pager) {
        StringBuilder builder = new StringBuilder("select * from business_merchant_info m ");
        builder.append(" where 1 = 1 ");
        Map<String, Object> params = new HashMap<>();
        builder.append(" and m.proxyUserId= :proxyUserId");
        params.put("proxyUserId", proxyId);
        if (!StringUtils.isEmpty(applyStatua)) {
            builder.append(" and m.applyStatus = :applyStatus");
            params.put("applyStatus", applyStatua);
        }

        if (!StringUtils.isEmpty(beginTime)) {
            builder.append(" and m.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(beginTime,"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(endTime)) {
            builder.append(" and m.createTime <= :endTime");
            params.put("endTime", DateUtil.stringToDate(endTime,"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(companyName)) {
            builder.append(" and").append(" m.companyName like ").append(":companyName");
            params.put("companyName", "%" + companyName + "%");
        }

        builder.append(" order by m.createTime desc");
        Page<MerchantInfo> page = findPage(builder.toString(), params, MerchantInfo.class, pager.of());
        return page;
    }


    /**
     * 开户管理
     * @param openAccountReq
     * @param pager
     * @return
     */
    @Override
    public Page<OpenAccountVo> findOpenAccount(OpenAccountReq openAccountReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select p.account as proxyAccount,m.proxyUserId as proxyUserId,m.createTime as createTime," +
                "m.userId as merchantUserId,u.account as merchantAccount,m.companyName as companyName,m.applyStatus as applyStatus " +
                " from business_merchant_info m LEFT JOIN security_user u on m.userId=u.id LEFT JOIN security_user p \n" +
                "on m.proxyUserId=p.id ");
        builder.append(" where 1=1 ");

        StringBuffer sqlCount=new StringBuffer("select count(1) from business_merchant_info m LEFT JOIN security_user u on m.userId=u.id LEFT JOIN security_user p \n" +
                " on m.proxyUserId=p.id");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(openAccountReq.getBeginTime())) {
            builder.append(" and m.createTime >= :beginTime");
            sqlCount.append(" and m.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(openAccountReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(openAccountReq.getEndTime())) {
            builder.append(" and m.createTime <= :endTime");
            sqlCount.append(" and m.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(openAccountReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(openAccountReq.getApplyStatus())) {
            builder.append(" and m.applyStatus = :applyStatus");
            sqlCount.append(" and m.applyStatus = :applyStatus");
            params.put("applyStatus",openAccountReq.getApplyStatus());
        }

        if (!StringUtils.isEmpty(openAccountReq.getQueryParam())) {
            builder.append(" and").append(" u.account like ").append(":queryParam");
            sqlCount.append(" and").append(" u.account like ").append(":queryParam");
            params.put("queryParam", "%" + openAccountReq.getQueryParam() + "%");
        }

        if (!StringUtils.isEmpty(openAccountReq.getProxyAccount())) {
            builder.append(" and").append(" p.account like ").append(":proxyAccount");
            sqlCount.append(" and").append(" p.account like ").append(":proxyAccount");
            params.put("proxyAccount", "%" + openAccountReq.getProxyAccount() + "%");
        }
        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by m.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<OpenAccountVo> list = findList(builder.toString(), params, OpenAccountVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    /**
     * 审核通过
     * @param id
     * @return
     */
    @Override
    @Transactional
    public boolean applyOpenPass(Long id) {
        int i=getRepository().applyMerchangInfoPass(id, ApplyMerchantEnum.PASS.toString());
        if(i>0){
            MerchantInfo merchantInfo=getRepository().findMerchantInfoById(id);
            if(merchantInfo!=null){
                int j=userRepository.updateStatus(merchantInfo.getUserId(),0);
                if(j>0){
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean batchBehalfOrder(BatchBehalfOrderDataVo batchBehalfOrderDataVo, long merchantUserId,String notifyUrl) {
        MerchantInfo merchantInfo=getRepository().findByUserId(merchantUserId);
        Assert.notNull(merchantInfo,"商户信息不存在,无法批量发起代付。");

        BigDecimal batchTotalAmount=BigDecimal.ZERO;
        if(CollectionUtils.isEmpty(batchBehalfOrderDataVo.getAllBatchBehalfData())){
            throw new IllegalArgumentException("内容不能为空");
        }
        int checkedFalse=0;
        BehalfBankCardResp behalfBankCard;
        List<BatchBehalfDataVo> allBatchBehalfData=new ArrayList<>();
        for(BatchBehalfDataVo batchBehalfDataVo:batchBehalfOrderDataVo.getAllBatchBehalfData()){
            if(batchBehalfDataVo.isChecked()){
                Assert.hasText(batchBehalfDataVo.getRealName(),"请填写勾选中的持卡人姓名");
                Assert.hasText(batchBehalfDataVo.getBankName(),"请填写勾选中的开户行名称");
                Assert.hasText(batchBehalfDataVo.getBankNo(),"请填写勾选中的银行卡号");
                Assert.notNull(batchBehalfDataVo.getAmount(),"请填写勾选中代付金额");
                BigDecimal intAmount =batchBehalfDataVo.getAmount().setScale(2,BigDecimal.ROUND_DOWN);
                if(batchBehalfDataVo.getAmount().compareTo(intAmount)!=0){
                    throw new IllegalArgumentException("金额仅支持2位小数");
                }
                if(intAmount.compareTo(BigDecimal.ZERO)<=0){
                    throw new IllegalArgumentException("批量下单的金额必须大于0");
                }
                behalfBankCard=new BehalfBankCardResp();
                behalfBankCard = behalfBankCardService.matchBehalfBankCard(merchantUserId,false);
                Assert.notNull(behalfBankCard,"创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：BEHALFBANKCARD");
                BigDecimal rateAmount = behalfBankCard.getMerchantRate().multiply(batchBehalfDataVo.getAmount());//手续费金额
                BigDecimal feeAmount=behalfBankCard.getMerchantFee();//收取商户单笔

                BigDecimal totalFee = batchBehalfDataVo.getAmount().add(rateAmount).add(feeAmount);//该订单总需要扣除的费用
                batchTotalAmount=batchTotalAmount.add(totalFee);

                batchBehalfDataVo.setBehalfBankCardResp(behalfBankCard);
                allBatchBehalfData.add(batchBehalfDataVo);
            }else{
                checkedFalse++;
            }
        }
        if(checkedFalse==11){
            throw new IllegalArgumentException("您没有勾选需要批量的数据无需提交！");
        }
        Wallet wallet = walletService.findWallet(merchantInfo.getUserId());
        if(wallet.getMoney().compareTo(batchTotalAmount)==-1){
            throw new IllegalArgumentException("通道余额无法满足批量下单的额度");
        }

         BehalfOrder behalfOrder;
         BehalfOrderCreateBankCardParams behalfOrderCreateBankCardParams;
         for(BatchBehalfDataVo batchBehalfDataVo : allBatchBehalfData){
             if(batchBehalfDataVo.isChecked()){
                 BigDecimal rateAmount = batchBehalfDataVo.getBehalfBankCardResp().getMerchantRate().multiply(batchBehalfDataVo.getAmount());//手续费金额
                 BigDecimal feeAmount=batchBehalfDataVo.getBehalfBankCardResp().getMerchantFee();//收取商户单笔
                 BigDecimal totalFee =batchBehalfDataVo.getAmount().add(rateAmount).add(feeAmount);//该订单总需要扣除的费用
                 behalfOrderCreateBankCardParams =new BehalfOrderCreateBankCardParams();
                 behalfOrderCreateBankCardParams.setRealName(batchBehalfDataVo.getRealName());
                 behalfOrderCreateBankCardParams.setBankName(batchBehalfDataVo.getBankName());
                 behalfOrderCreateBankCardParams.setBankNo(batchBehalfDataVo.getBankNo());
                 behalfOrderCreateBankCardParams.setBankBranch(batchBehalfDataVo.getBankBranch());
                 behalfOrder=new BehalfOrder();

                 int i = walletService.subMoney(merchantInfo.getUserId(), totalFee, WalletLogEnum.MERCHANTBATCHBEHALF);
                 if(i<=0){
                     throw new IllegalArgumentException("商户余额不足");
                 }
                 behalfOrder.setOrderNo(BehalfOrder.genaralOrderNo("DF"));
                 behalfOrder.setOutOrderNo(NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
                 behalfOrder.setAmount(batchBehalfDataVo.getAmount());
                 behalfOrder.setRateAmount(rateAmount);
                 behalfOrder.setFeeAmount(feeAmount);
                 behalfOrder.setMerchantUserId(merchantInfo.getUserId());
                 behalfOrder.setStatus(OrderStatusEnum.PROCESS);
                 behalfOrder.setStatusDesc("订单创建成功,处理中");
                 behalfOrder.setAttach("");
                 behalfOrder.setNotifyUrl(notifyUrl);
                 behalfOrder.setBankCardInfoJson(JSON.toJSONString(behalfOrderCreateBankCardParams));
                 behalfOrder.setBehalfBankCardInfoJson(JSON.toJSONString(batchBehalfDataVo.getBehalfBankCardResp()));
                 behalfOrder.setMerchantBankNo(batchBehalfDataVo.getBankNo());
                 behalfOrder.setBehalfBankNo(batchBehalfDataVo.getBehalfBankCardResp().getBankNo());
                 behalfOrder.setBehalfUserId(batchBehalfDataVo.getBehalfBankCardResp().getBehalfUserId());
                 behalfOrderService.getRepository().save(behalfOrder);
             }
            }
        return true;
    }

    @Override
    public Page<MerchantInfoVo> findListBehalfMerchantInfo(MerchantQueryReq agencyQueryReq, long behalfUserId, Pager pager) {
        StringBuilder builder = new StringBuilder("select u.account as merchantAccount,w.money as walletMoney from business_behalf_bankcardgrouprelation r " +
                " LEFT JOIN business_behalf_bankcardgroup p on r.bankCardGroupId=p.id \n" +
                " LEFT JOIN security_user u on r.merchantUserId=u.id LEFT JOIN " +
                "business_wallet w on r.merchantUserId=w.userId ");
        builder.append("  where 1=1 and p.behalfUserId=:behalfUserId");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_behalf_bankcardgrouprelation r LEFT JOIN business_behalf_bankcardgroup p on r.bankCardGroupId=p.id \n" +
                "LEFT JOIN security_user u on r.merchantUserId=u.id LEFT JOIN business_wallet w on r.merchantUserId=w.userId ");
        sqlCount.append(" where 1=1 and p.behalfUserId=:behalfUserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("behalfUserId",behalfUserId);
        if (!StringUtils.isEmpty(agencyQueryReq.getQueryParam())) {
            builder.append("  and u.account like :account");
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+agencyQueryReq.getQueryParam()+'%');
        }
        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by u.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<MerchantInfoVo> list = findList(builder.toString(), params, MerchantInfoVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public MerchantInfoTotalVo findBehalfMerchantTotal(MerchantQueryReq merchantQueryReq, long behalfUserId) {
        StringBuffer sqlCount=new StringBuffer(" select ifnull(sum(w.money),0) as totalBalance from business_behalf_bankcardgrouprelation r LEFT JOIN " +
                " business_behalf_bankcardgroup p on r.bankCardGroupId=p.id \n" +
                "LEFT JOIN security_user u on r.merchantUserId=u.id LEFT JOIN business_wallet w on r.merchantUserId=w.userId");
        sqlCount.append(" where 1=1  and p.behalfUserId=:behalfUserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("behalfUserId",behalfUserId);
        if (!StringUtils.isEmpty(merchantQueryReq.getQueryParam())) {
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+merchantQueryReq.getQueryParam()+'%');
        }
        return findOne(sqlCount.toString(),params,MerchantInfoTotalVo.class);
    }

    @Override
    @Transactional
    public boolean bulkTextBehalfOrder(List<BatchBehalfDataVo> batchBehalfDataVoList, long merchantUserId, String notifyUrl) {
        MerchantInfo merchantInfo=getRepository().findByUserId(merchantUserId);
        Assert.notNull(merchantInfo,"商户信息不存在,无法批量文本导入发起代付。");

        BehalfBankCardResp behalfBankCard = behalfBankCardService.matchBehalfBankCard(merchantUserId,false);
        Assert.notNull(behalfBankCard,"创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：BEHALFBANKCARD");

        BigDecimal batchTotalAmount=BigDecimal.ZERO;

        for (BatchBehalfDataVo batchBehalfDataVo : batchBehalfDataVoList) {
            BigDecimal rateAmount = behalfBankCard.getMerchantRate().multiply(batchBehalfDataVo.getAmount());//手续费金额
            BigDecimal feeAmount=behalfBankCard.getMerchantFee();//收取商户单笔
            BigDecimal totalFee = batchBehalfDataVo.getAmount().add(rateAmount).add(feeAmount);//该订单总需要扣除的费用
            batchTotalAmount=batchTotalAmount.add(totalFee);
            batchBehalfDataVo.setBehalfBankCardResp(behalfBankCard);
        }


        Wallet wallet = walletService.findWallet(merchantInfo.getUserId());
        if(wallet.getMoney().compareTo(batchTotalAmount)==-1){
            throw new IllegalArgumentException("通道余额无法满足批量下单的额度");
        }

        BehalfOrder behalfOrder;
        BehalfOrderCreateBankCardParams behalfOrderCreateBankCardParams;
        for(BatchBehalfDataVo batchBehalfDataVo : batchBehalfDataVoList){
                BigDecimal rateAmount = batchBehalfDataVo.getBehalfBankCardResp().getMerchantRate().multiply(batchBehalfDataVo.getAmount());//手续费金额
                BigDecimal feeAmount=batchBehalfDataVo.getBehalfBankCardResp().getMerchantFee();//收取商户单笔
                BigDecimal totalFee =batchBehalfDataVo.getAmount().add(rateAmount).add(feeAmount);//该订单总需要扣除的费用
                behalfOrderCreateBankCardParams =new BehalfOrderCreateBankCardParams();
                behalfOrderCreateBankCardParams.setRealName(batchBehalfDataVo.getRealName());
                behalfOrderCreateBankCardParams.setBankName(batchBehalfDataVo.getBankName());
                behalfOrderCreateBankCardParams.setBankNo(batchBehalfDataVo.getBankNo());
                behalfOrderCreateBankCardParams.setBankBranch(batchBehalfDataVo.getBankBranch());
                behalfOrder=new BehalfOrder();

                int i = walletService.subMoney(merchantInfo.getUserId(), totalFee, WalletLogEnum.MERCHANTBATCHBEHALF);
                if(i<=0){
                    throw new IllegalArgumentException("商户余额不足");
                }
                behalfOrder.setOrderNo(BehalfOrder.genaralOrderNo("DF"));
                behalfOrder.setOutOrderNo(NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
                behalfOrder.setAmount(batchBehalfDataVo.getAmount());
                behalfOrder.setRateAmount(rateAmount);
                behalfOrder.setFeeAmount(feeAmount);
                behalfOrder.setMerchantUserId(merchantInfo.getUserId());
                behalfOrder.setStatus(OrderStatusEnum.PROCESS);
                behalfOrder.setStatusDesc("订单创建成功,处理中");
                behalfOrder.setAttach("");
                behalfOrder.setNotifyUrl(notifyUrl);
                behalfOrder.setBankCardInfoJson(JSON.toJSONString(behalfOrderCreateBankCardParams));
                behalfOrder.setBehalfBankCardInfoJson(JSON.toJSONString(batchBehalfDataVo.getBehalfBankCardResp()));
                behalfOrder.setMerchantBankNo(batchBehalfDataVo.getBankNo());
                behalfOrder.setBehalfBankNo(batchBehalfDataVo.getBehalfBankCardResp().getBankNo());
                behalfOrder.setBehalfUserId(batchBehalfDataVo.getBehalfBankCardResp().getBehalfUserId());
                behalfOrderService.getRepository().save(behalfOrder);

        }
        return true;
    }

}

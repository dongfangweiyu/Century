package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.entity.business.BehalfBankCardWalletLog;
import com.ra.dao.repository.business.BehalfBankCardWalletLogRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.WalletLogVo;
import com.ra.service.business.BehalfBankCardService;
import com.ra.service.business.BehalfBankCardWalletLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehalfBankCardWalletLogServiceImpl extends BaseServiceImpl<BehalfBankCardWalletLogRepository> implements BehalfBankCardWalletLogService {
    @Autowired
    BehalfBankCardService behalfBankCardService;
    @Override
    public boolean saveBehalfBankCardLog(long bankCardId, BigDecimal amount, BehalfWalletLogEnum behalfWalletLogEnum) {
        BehalfBankCard behalfBankCard = behalfBankCardService.getRepository().findById(bankCardId);
        Assert.notNull(behalfBankCard,"银行卡不存在");

        //如果变动是0元就不增加记录，直接返回true
        if(amount.compareTo(BigDecimal.ZERO)==0){
            return true;
        }

        BehalfBankCardWalletLog log=new BehalfBankCardWalletLog();
        log.setAmount(amount);
        log.setBehalfUserId(behalfBankCard.getBehalfUserId());
        log.setBankNo(behalfBankCard.getBankNo());
        log.setDescription(behalfWalletLogEnum.getDescription());
        log.setBalance(behalfBankCard.getBalance().add(amount));
        log.setBeforeBalance(behalfBankCard.getBalance());
        log.setLogEnum(behalfWalletLogEnum);
        if(amount.compareTo(BigDecimal.ZERO)==1){
            log.setWay("rollIn");
        }
        if(amount.compareTo(BigDecimal.ZERO)==-1){
            log.setWay("rollOut");
        }
        repository.save(log);
        return true;
    }

    @Override
    public Page<BehalfBankCardWalletLog> findBankCardLogList(WalletLogReq walletLogReq, long behalfuserId, Pager pager) {
        StringBuilder builder = new StringBuilder("select w.* from business_behalf_bankcard_wallet_log w ");
        builder.append("  where 1=1 and w.behalfUserId=:behalfuserId");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_behalf_bankcard_wallet_log w " );
        sqlCount.append(" where 1=1 and w.behalfUserId=:behalfuserId ");
        Map<String, Object> params = new HashMap<>();
        params.put("behalfuserId",behalfuserId);
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            sqlCount.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
        }
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) { //银行卡查询条件
            builder.append("  and w.bankNo = :bankNo");
            sqlCount.append("  and w.bankNo = :bankNo");
            params.put("bankNo",walletLogReq.getQueryParam());
        }
        if (!StringUtils.isEmpty(walletLogReq.getLogEnum())) {
            builder.append("  and w.logEnum = :logEnum");
            sqlCount.append("  and w.logEnum = :logEnum");
            params.put("logEnum",walletLogReq.getLogEnum());
        }
        if (!StringUtils.isEmpty(walletLogReq.getBeginTime())) {
            builder.append(" and w.createTime >= :beginTime");
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(walletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(walletLogReq.getEndTime())) {
            builder.append(" and w.createTime <= :endTime");
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(walletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by w.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<BehalfBankCardWalletLog> list = findList(builder.toString(), params, BehalfBankCardWalletLog.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findByBehalfBankCardTotal(WalletLogReq walletLogReq,long behalfUserId) {
        StringBuilder builder = new StringBuilder("select ifnull(sum(w.amount),0) from business_behalf_bankcard_wallet_log w  ");
        builder.append("  where 1=1 and w.behalfUserId=:behalfUserId");
        Map<String, Object> params = new HashMap<>();
        params.put("behalfUserId",behalfUserId);
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
        }
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) { //银行卡查询条件
            builder.append("  and w.bankNo = :bankNo");
            params.put("bankNo",walletLogReq.getQueryParam());
        }
        if (!StringUtils.isEmpty(walletLogReq.getLogEnum())) {
            builder.append("  and w.logEnum = :logEnum");
            params.put("logEnum",walletLogReq.getLogEnum());
        }
        if (!StringUtils.isEmpty(walletLogReq.getBeginTime())) {
            builder.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(walletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(walletLogReq.getEndTime())) {
            builder.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(walletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        double sum = findSum(builder.toString(), params);
        return BigDecimal.valueOf(sum);
    }

}

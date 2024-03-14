package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.entity.business.BehalfInfo;
import com.ra.dao.entity.business.BehalfProfitLog;
import com.ra.dao.repository.business.BehalfProfitLogRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.BehalfProfitLogVo;
import com.ra.service.business.BehalfInfoService;
import com.ra.service.business.BehalfProfitLogService;
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
public class BehalfProfitLogServiceImpl extends BaseServiceImpl<BehalfProfitLogRepository> implements BehalfProfitLogService {

    @Autowired
    BehalfInfoService behalfInfoService;
    @Override
    public boolean saveBehalfProfitLog(long behalfUserId, BigDecimal amount, String remark) {
        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(behalfUserId);
        Assert.notNull(behalfInfo,"卡商不存在");
        //如果变动是0元就不增加记录，直接返回true
        if(amount.compareTo(BigDecimal.ZERO)==0){
            return true;
        }

        BehalfProfitLog log=new BehalfProfitLog();
        log.setAmount(amount);
        log.setBehalfUserId(behalfUserId);
        log.setDescription(remark);
        log.setBalance(behalfInfo.getProfit().add(amount));
        log.setBeforeBalance(behalfInfo.getProfit());
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
    public Page<BehalfProfitLogVo> findBehalfProfitLogList(WalletLogReq walletLogReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select w.*,u.account from business_behalf_profit_log w LEFT JOIN security_user u on w.behalfUserId=u.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_behalf_profit_log w LEFT JOIN security_user u on w.behalfUserId=u.id " );
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            sqlCount.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
        }
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) {
            builder.append("  and u.account = :account");
            sqlCount.append("  and u.account = :account");
            params.put("account",walletLogReq.getQueryParam());
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
        List<BehalfProfitLogVo> list = findList(builder.toString(), params, BehalfProfitLogVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findByBehalfProfitTotal(WalletLogReq walletLogReq) {
        StringBuilder builder = new StringBuilder("select ifnull(sum(w.amount),0) from business_behalf_profit_log w LEFT JOIN security_user u on w.behalfUserId=u.id ");
        builder.append("  where 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
        }
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) {
            builder.append("  and u.account = :account");
            params.put("account",walletLogReq.getQueryParam());
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

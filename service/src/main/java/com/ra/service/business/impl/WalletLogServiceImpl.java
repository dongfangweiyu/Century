package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.business.WalletLog;
import com.ra.dao.repository.business.WalletLogRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.MerchantInfoVo;
import com.ra.service.bean.resp.WalletLogTimeVo;
import com.ra.service.bean.resp.WalletLogVo;
import com.ra.service.business.WalletLogService;
import com.ra.service.business.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ouyang
 * @time 2019-01-24
 * @description
 */
@Service
public class WalletLogServiceImpl extends BaseServiceImpl<WalletLogRepository> implements WalletLogService {

    @Autowired
    private WalletService walletService;

    @Override
    public Page<WalletLog> findWalletLogWithdraw(long userId, Pager pager) {
        String sql="select logEnum,createTime,amount,description from business_wallet_log where logEnum='COMMISION_WITHDRAW' and userId="+userId+" order by createTime desc";
        return findPage(sql,new HashMap<>(),WalletLog.class,pager.of());
    }

    @Override
    public boolean saveWalletLog(long userId, BigDecimal amount, WalletLogEnum logEnum) {
        Wallet wallet = walletService.getRepository().findWalletByUserId(userId);
        return saveWalletLog(wallet,amount,logEnum);
    }

    @Override
    public boolean saveWalletLog(Wallet wallet, BigDecimal amount, WalletLogEnum logEnum) {
        Assert.notNull(wallet,"用户钱包不存在");
        //如果变动是0元就不增加记录，直接返回true
        if(amount.compareTo(BigDecimal.ZERO)==0){
            return true;
        }

        WalletLog log=new WalletLog();
        log.setAmount(amount);
        log.setWalletId(wallet.getId());
        log.setUserId(wallet.getUserId());
        log.setLogEnum(logEnum);
        log.setDescription(logEnum.getDescription());
        log.setBalance(wallet.getMoney().add(amount));
        log.setBeforeBalance(wallet.getMoney());
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
    public Page<WalletLogVo> findWalletLogList(WalletLogReq walletLogReq, long userId, Pager pager) {
        StringBuilder builder = new StringBuilder("select u.account as account,w.* from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        builder.append("  where 1=1 and w.userId=:userId");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        sqlCount.append(" where 1=1 and w.userId=:userId ");
        Map<String, Object> params = new HashMap<>();
        params.put("userId",userId);
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            sqlCount.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
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
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by w.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<WalletLogVo> list = findList(builder.toString(), params, WalletLogVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public Page<WalletLogVo> findAdminWalletLogList(WalletLogReq walletLogReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select u.account as account,w.* from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            builder.append("  and w.way = :way");
            sqlCount.append("  and w.way = :way");
            params.put("way",walletLogReq.getWay());
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
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) {
            builder.append("  and u.account = :account");
            sqlCount.append("  and u.account = :account");
            params.put("account",walletLogReq.getQueryParam());
        }

        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by w.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<WalletLogVo> list = findList(builder.toString(), params, WalletLogVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public Page<WalletLogTimeVo> findWalletLogTimeList(WalletLogReq walletLogReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select b.* from (select u.account as account,w.userId,w.balance,w.createTime from business_wallet_log w  \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" SELECT COUNT(1) FROM ( select w.userId from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

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
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) {
            builder.append("  and u.account like :account");
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+walletLogReq.getQueryParam()+'%');
        }

        sqlCount.append("  group by w.userId  ) b ");
        int count = findCount(sqlCount.toString(), params);

        builder.append(" order by w.id desc) b GROUP BY b.userId limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<WalletLogTimeVo> list = findList(builder.toString(), params, WalletLogTimeVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findWalletLogTimeTotal(WalletLogReq walletLogReq) {
        StringBuffer sqlCount=new StringBuffer(" SELECT IFNULL(SUM(c.balance),0) FROM (SELECT b.* FROM ( select w.balance,w.userId from business_wallet_log w \n" +
                " LEFT JOIN security_user u on w.userId=u.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(walletLogReq.getBeginTime())) {
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(walletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(walletLogReq.getEndTime())) {
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(walletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(walletLogReq.getQueryParam())) {
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+walletLogReq.getQueryParam()+'%');
        }

        sqlCount.append(" order by w.id desc  ) b group by b.userId ) c ");
        double sum = findSum(sqlCount.toString(), params);
        return BigDecimal.valueOf(sum);
    }

    @Override
    public double findByWalletLogTotal(WalletLogReq walletLogReq, long userId) {
        Map<String, Object> params = new HashMap<>();
        StringBuffer sql = new StringBuffer(" select ifnull(sum(l.amount),0) from business_wallet_log l where l.userId= :userId ");
        params.put("userId",userId);
        if (!StringUtils.isEmpty(walletLogReq.getWay())) {
            sql.append(" and l.way = :way");
            params.put("way",walletLogReq.getWay());
        }
        if (!StringUtils.isEmpty(walletLogReq.getBeginTime())) {
            sql.append(" and l.createTime >= :beginTime");
            params.put("beginTime",  DateUtil.stringToDate(walletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(walletLogReq.getEndTime())) {
            sql.append(" and l.createTime <= :endTime");
            params.put("endTime", DateUtil.stringToDate(walletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(walletLogReq.getLogEnum())) {
            sql.append(" and l.logEnum = :logEnum");
            params.put("logEnum",walletLogReq.getLogEnum());
        }
        return findSum(sql.toString(),params);
    }

}

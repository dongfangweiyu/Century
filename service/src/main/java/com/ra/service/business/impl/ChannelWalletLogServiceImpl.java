package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.entity.business.ChannelWalletLog;
import com.ra.dao.entity.business.WalletLog;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.dao.repository.business.ChannelWalletLogRepository;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.ChannelWalletLogReq;
import com.ra.service.bean.resp.ChannelWalletLogTimeVo;
import com.ra.service.bean.resp.ChannelWalletLogVo;
import com.ra.service.bean.resp.WalletLogTimeVo;
import com.ra.service.business.ChannelWalletLogService;
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
public class ChannelWalletLogServiceImpl extends BaseServiceImpl<ChannelWalletLogRepository> implements ChannelWalletLogService {

    @Autowired
    private ConfigPayInterfaceRepository configPayInterfaceRepository;

    @Override
    public Page<ChannelWalletLogVo> findChannelWalletLogList(ChannelWalletLogReq channelWalletLogReq, Pager pager) {
        StringBuffer sql=new StringBuffer("select w.*,c.configName from business_channel_wallet_log as w left join config_payInterface as c on w.configPayInterfaceId=c.id where 1=1 ");

        Map<String, Object> params = new HashMap<>();

        if(channelWalletLogReq.getConfigPayInterfaceId()!=null){
            sql.append(" and w.configPayInterfaceId = :configPayInterfaceId");
            params.put("configPayInterfaceId",channelWalletLogReq.getConfigPayInterfaceId());
        }

        if (!StringUtils.isEmpty(channelWalletLogReq.getBeginTime())) {
            sql.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(channelWalletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(channelWalletLogReq.getEndTime())) {
            sql.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(channelWalletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if(!StringUtils.isEmpty(channelWalletLogReq.getWay())){
            if(channelWalletLogReq.getWay().equals("rollIn")){
                sql.append(" and w.amount >= 0  ");
            }else{
                sql.append(" and w.amount < 0 ");
            }
        }

        sql.append(" order by w.id desc");
        return findPage(sql.toString(),params,ChannelWalletLogVo.class,pager.of());
    }

    @Override
    public boolean saveWalletLog(long configPayInterfaceId, BigDecimal amount, String description) {

        ConfigPayInterface channelWallet = configPayInterfaceRepository.findById(configPayInterfaceId);
        Assert.notNull(channelWallet,"通道钱包不存在");

        //如果变动是0元就不增加记录，直接返回true
        if(amount.compareTo(BigDecimal.ZERO)==0){
            return true;
        }

        ChannelWalletLog log=new ChannelWalletLog();
        log.setAmount(amount);
        log.setConfigPayInterfaceId(configPayInterfaceId);
        log.setDescription(description);
        log.setBalance(channelWallet.getMoney().add(amount));
        log.setBeforeBalance(channelWallet.getMoney());
        repository.save(log);
        return true;
    }

    @Override
    public Page<ChannelWalletLogTimeVo> findChannelWalletLogTimeList(ChannelWalletLogReq channelWalletLogReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select b.* from (select u.configName as configName,w.configPayInterfaceId,w.balance,w.createTime from business_channel_wallet_log w  \n" +
                " LEFT JOIN config_payInterface u on w.configPayInterfaceId=u.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" SELECT COUNT(1) FROM ( select w.configPayInterfaceId from business_channel_wallet_log w \n" +
                " LEFT JOIN config_payInterface u on w.configPayInterfaceId=u.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(channelWalletLogReq.getBeginTime())) {
            builder.append(" and w.createTime >= :beginTime");
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(channelWalletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(channelWalletLogReq.getEndTime())) {
            builder.append(" and w.createTime <= :endTime");
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(channelWalletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(channelWalletLogReq.getConfigPayInterfaceId())) {
            builder.append("  and u.id = :configPayInterfaceId");
            sqlCount.append("  and u.id = :configPayInterfaceId");
            params.put("configPayInterfaceId", channelWalletLogReq.getConfigPayInterfaceId());
        }

        sqlCount.append("  group by w.configPayInterfaceId  ) b ");
        int count = findCount(sqlCount.toString(), params);

        builder.append(" order by w.id desc) b GROUP BY b.configPayInterfaceId limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<ChannelWalletLogTimeVo> list = findList(builder.toString(), params, ChannelWalletLogTimeVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BigDecimal findChannelWalletLogTimeTotal(ChannelWalletLogReq channelWalletLogReq) {
        StringBuffer sqlCount=new StringBuffer(" SELECT IFNULL(SUM(c.balance),0) FROM (SELECT b.* FROM ( select w.balance,w.configPayInterfaceId from business_channel_wallet_log w \n" +
                " LEFT JOIN config_payInterface u on w.configPayInterfaceId=u.id ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(channelWalletLogReq.getBeginTime())) {
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(channelWalletLogReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        if (!StringUtils.isEmpty(channelWalletLogReq.getEndTime())) {
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(channelWalletLogReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(channelWalletLogReq.getConfigPayInterfaceId())) {
            sqlCount.append("  and u.id = :configPayInterfaceId");
            params.put("configPayInterfaceId", channelWalletLogReq.getConfigPayInterfaceId());
        }

        sqlCount.append("  order by w.id desc  ) b group by b.configPayInterfaceId ) c");
        double sum = findSum(sqlCount.toString(), params);
        return BigDecimal.valueOf(sum);
    }

}

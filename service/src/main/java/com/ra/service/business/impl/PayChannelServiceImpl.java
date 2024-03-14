package com.ra.service.business.impl;

import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.business.PayCode2Channel;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.repository.business.PayChannelRepository;
import com.ra.dao.repository.business.PayCode2ChannelRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.resp.PayChannelVo;
import com.ra.service.bean.resp.RateVo;
import com.ra.service.business.PayChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PayChannelServiceImpl extends BaseServiceImpl<PayChannelRepository> implements PayChannelService {

    private static final Logger logger= LoggerFactory.getLogger(PayChannelServiceImpl.class);

    @Autowired
    private PayCode2ChannelRepository payCode2ChannelRepository;

    @Override
    @Transactional
    public boolean savePayChannel(PayChannel payChannel, String[] payCodeIds) throws Exception{
        if(payChannel.getId()!=null&&payChannel.getId()>0){
            payCode2ChannelRepository.deleteByPayChannelId(payChannel.getId());
        }

        payChannel.setRate(payChannel.getRate().divide(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP));
        PayChannel save = getRepository().save(payChannel);
        if(save!=null){
            PayCode2Channel payCode2Channel=null;
            List<PayCode2Channel> list=new ArrayList<>();
            for (String payCodeId : payCodeIds) {
                payCode2Channel= new PayCode2Channel();
                payCode2Channel.setPayChannelId(save.getId());
                payCode2Channel.setPayCodeId(Long.parseLong(payCodeId));
                list.add(payCode2Channel);
            }
            payCode2ChannelRepository.saveAll(list);
            return true;
        }
        return false;
    }


    @Override
    public PayChannel matchChannel(BigDecimal amount, long merchantInfoId, long payCodeId) {
        Map<String,Object> params=new HashMap<>();
        params.put("merchantInfoId",merchantInfoId);
        params.put("payCodeId",payCodeId);
        params.put("amount",amount);
        String sql="select a.*,b.weight from business_payChannel as a left join business_rate as b on a.id=b.payChannelId " +
                "left join business_payCode2Channel as c on a.id=c.payChannelId "+
                "where b.merchantInfoId=:merchantInfoId and c.payCodeId=:payCodeId and (a.rate <= b.merchantRate or a.rate <= b.proxyRate) and a.isEnable=1 " +
                "and (a.risk=0 or (a.risk=1 and a.creditScore>=:amount and a.minAmount<=:amount and a.maxAmount>=:amount and a.beginTime <= DATE_FORMAT(now(),'%T')) and a.endTime>= DATE_FORMAT(now(),'%T'))" +
                "";
//                "ORDER BY RAND()";
//        return findOne(sql,params,PayChannel.class);
        List<PayChannelVo> list = findList(sql, params, PayChannelVo.class);
        if(list.size()<=0){
            return null;
        }
        Random random = new Random();
        Integer weightSum = 0;
        PayChannel result=list.get(random.nextInt(list.size()));
        for (PayChannelVo wc : list) {
            weightSum += wc.getWeight();
        }
        if(weightSum>0){
            Integer n = random.nextInt(weightSum); // n in [0, weightSum)
            Integer m = 0;
            for (PayChannelVo wc : list) {
                if (m <= n && n < m + wc.getWeight()) {
                    result=wc;
                    break;
                }
                m += wc.getWeight();
            }
        }
        return result;
    }

    @Override
    public List<PayChannel> listPayChannelUserId(long id) {
        Map<String,Object> params=new HashMap<>();
        params.put("userId",id);
        String sql="SELECT p.* FROM business_rate r LEFT JOIN business_payChannel p on r.payChannelId=p.id where r.userId=:userId";
        return findList(sql, params, PayChannel.class);
    }
}

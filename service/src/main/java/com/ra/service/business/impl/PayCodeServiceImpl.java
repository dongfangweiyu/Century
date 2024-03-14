package com.ra.service.business.impl;

import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.repository.business.PayChannelRepository;
import com.ra.dao.repository.business.PayCodeRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayCodeService;
import org.hibernate.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.beans.ImmutableBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayCodeServiceImpl extends BaseServiceImpl<PayCodeRepository> implements PayCodeService {

    private static final Logger logger= LoggerFactory.getLogger(PayCodeServiceImpl.class);


    @Override
    public List<PayChannel> findPayChannelByPayCodeId(Long payCodeId) {
        Map<String,Object> params=new HashMap<>();
        params.put("payCodeId",payCodeId);
        String sql="select a.* from business_payChannel as a left join business_payCode2Channel as b on a.id=b.payChannelId where b.payCodeId=:payCodeId";
        return findList(sql, params,PayChannel.class);
    }

    @Override
    public List<PayCode> findPayCodeByChannel2Id(long payChannelId) {
        Map<String,Object> params=new HashMap<>();
        params.put("payChannelId",payChannelId);
        String sql="select a.* from business_payCode as a left join business_payCode2Channel as b on a.id=b.payCodeId where b.payChannelId=:payChannelId";
        return findList(sql, params,PayCode.class);
    }
}

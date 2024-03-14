package com.ra.service.business;

import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.repository.business.PayCodeRepository;
import com.ra.service.base.BaseService;

import java.util.List;

public interface PayCodeService extends BaseService<PayCodeRepository> {

    /**
     * 获取支付编码绑定的通道
     */
    List<PayChannel> findPayChannelByPayCodeId(Long payCodeId);

    /**
     * 获取支付编码，通过通道获取相应绑定的编码列表
     * @return
     */
    List<PayCode> findPayCodeByChannel2Id(long payChannelId);
}

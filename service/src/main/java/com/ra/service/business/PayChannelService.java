package com.ra.service.business;

import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.repository.business.PayChannelRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.resp.RateVo;

import java.math.BigDecimal;
import java.util.List;

public interface PayChannelService extends BaseService<PayChannelRepository> {

    /**
     * 保存支付通道和2code映射信息
     * @param payChannel
     * @param payCodeIds
     * @return
     * @throws Exception
     */
    boolean savePayChannel(PayChannel payChannel,String[] payCodeIds) throws Exception;


    /**
     * 匹配通道
     * @param amount 订单金额
     * @param merchantUserId 下单的商户id
     * @param payCodeId 支付编码
     * @return
     */
    PayChannel matchChannel(BigDecimal amount,long merchantUserId, long payCodeId);

    /**
     * 根据用户id查询出开通哪些列表
     * @param id
     * @return
     */
    List<PayChannel>listPayChannelUserId(long id);
}

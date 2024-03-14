package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 开户审核信息
 */
@Data
public class OpenApplyInfoVo {
    private boolean checkedBox;
    private String payChannelName;
    /**
     * 绑定的通道id
     */
    private Long payChannelId;

    /**
     * 对应的商户费率
     */
    private BigDecimal MerchantRate;

    /**
     * 对应的代理费率
     */
    private BigDecimal proxyRate;
}

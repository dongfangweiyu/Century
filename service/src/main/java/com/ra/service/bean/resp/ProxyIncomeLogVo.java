package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ProxyIncomeLogVo {
    private Long id;//收入ID
    private String orderNo;//订单号

    private String merchantAccount;//商户账户

    private BigDecimal amount;//接单金额

    private String payCode;//通道编码

    private String payChannelInfoJson;//匹配的通道信息Json

    private Timestamp createTime;//收入创建时间

    private String payChannelName;//通道名称

    private BigDecimal rate;//费率

    private BigDecimal incomeMoney;//收入金额

    private String description;

    private String incomeType;//收入类型
}

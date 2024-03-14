package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class MerchantInfoVo {

    /**
     * 商户的用户ID
     */
    private Long userId;

    private String merchantAccount;//商户账号

    private String companyName;//商户名称

    /**
     * 账户余额
     */
    private BigDecimal walletMoney;//钱包余额

    private Long proxyUserId;

    private String proxyAccount;//代理账号

    private Integer status;//状态

    /**
     * 创建时间
     */
    private Timestamp createTime;

    private Timestamp lastLoginTime;

    private String loginIp;
}

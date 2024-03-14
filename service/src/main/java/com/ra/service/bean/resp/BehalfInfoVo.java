package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BehalfInfoVo {

    /**
     * 卡商的用户ID
     */
    private Long userId;

    private String behalfAccount;//商户账号

    private String companyName;//卡商名称

    /**
     * 账户余额
     */
    private BigDecimal walletMoney;//钱包余额

    private BigDecimal profit;//剩余利润

    private Integer status;//状态

    /**
     * 创建时间
     */
    private Timestamp createTime;

    private Timestamp lastLoginTime;

    private String loginIp;

    private BigDecimal behalfRate;

    private BigDecimal behalfFee;
}

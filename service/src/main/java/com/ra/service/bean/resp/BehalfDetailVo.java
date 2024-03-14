package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 卡商详情
 */
@Data
public class BehalfDetailVo {
    private Long behalfUserId;

    /**
     * 卡商账户
     */
    private String behalfAccount;
    /**
     * 商户名称
     */
    private String companyName;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 商户状态 0正常  1 禁用
     */
    private Integer status;


    /**
     * 账户余额
     */
    private BigDecimal walletMoney;

    /**
     * 下发费率
     */
    private BigDecimal behalfRate;

    /**
     * 单笔费用
     */
    private BigDecimal behalfFee;

    /**
     * 卡商编号
     */
    private String appId;

    /**
     * 密钥
     */
    private String secret;
}

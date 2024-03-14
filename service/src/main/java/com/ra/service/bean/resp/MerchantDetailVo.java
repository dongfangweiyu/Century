package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 商户详情
 */
@Data
public class MerchantDetailVo {
    private Long merchantUserId;

    /**
     * 商户账户
     */
    private String merchantAccount;
    /**
     * 属于哪个代理的商户
     * 业务员代理的userId
     */
    private String proxyAccount;
    /**
     * 商户名称
     */
    private String companyName;


    /**
     * appId
     * 商户编号
     */
    private String appId;

    /**
     * 秘钥
     */
    private String secret;

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
}

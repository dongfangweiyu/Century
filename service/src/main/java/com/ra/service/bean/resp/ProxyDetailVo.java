package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ProxyDetailVo {

    private Long proxyUserId;

    /**
     * 代理账户
     */
    private String proxyAccount;

    /**
     * 代理名称
     */
    private String companyName;


    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 代理状态 0正常  1 禁用
     */
    private Integer status;


    /**
     * 账户余额
     */
    private BigDecimal walletMoney;
}

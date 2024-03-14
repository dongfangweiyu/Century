package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class ProxyInfoVo {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 账号
     */
    private String account;


    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 最后登录时间
     */
    private Timestamp lastLoginTime;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 是否禁用
     */
    private int status;
    /**
     * 账户余额
     */
    private BigDecimal walletMoney;
}

package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 开户管理
 */
@Data
public class OpenAccountVo {
    private Long merchantUserId;

    private String proxyAccount;

    private String proxyUserId;

    private String merchantAccount;

    private String companyName;

    private Timestamp createTime;

    private String applyStatus;

    private String  merchantDesc;
}

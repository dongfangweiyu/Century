package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BehalfProfitLogVo {
    private String account;//账户

    private BigDecimal amount;//变动金额

    private BigDecimal beforeBalance;//变动前的余额

    private BigDecimal balance;//变动后的余额

    private String description;//描述

    private String way;//变动方式

    private Timestamp createTime;

}

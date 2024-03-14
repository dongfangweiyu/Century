package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BehalfInfoTotalVo {
    /**
     * 总余额
     */
    private BigDecimal totalBalance;
}

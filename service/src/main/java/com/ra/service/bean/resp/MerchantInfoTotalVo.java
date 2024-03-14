package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class MerchantInfoTotalVo {
    /**
     * 总余额
     */
    private BigDecimal totalBalance;
}

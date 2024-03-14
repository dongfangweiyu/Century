package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单统计明细
 */
@Data
public class IncomeTotalVo {
    private BigDecimal payIncomeTotal;//代收收入总金额

    private BigDecimal behalfIncomeTotal;//代付收入总金额


}

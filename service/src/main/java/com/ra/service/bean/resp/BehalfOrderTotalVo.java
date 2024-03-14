package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单统计明细
 */
@Data
public class BehalfOrderTotalVo {
    private BigDecimal behalfOrderMoneyTotal;//订单总金额

    private BigDecimal behalfOrderSuccessTotal;//订单成功总金额

    private Integer totalBehalfOrderCount;//总笔数

    private Integer behalfOrderSuccessCount;//成功笔数


}

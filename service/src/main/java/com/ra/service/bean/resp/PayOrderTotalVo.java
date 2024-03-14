package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单统计明细
 */
@Data
public class PayOrderTotalVo {
    private BigDecimal otcOrderSuccessTotal;//成功总金额

    private BigDecimal otcTodayCompleteTotal;//总回调金额

    private BigDecimal otcCrateTotal;//创建订单总额

    private BigDecimal otcSuccessCount;//成功笔数

}

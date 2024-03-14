package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 提现订单
 */
@Data
public class WithdrawOrderVo {

    private Long id;
    private String withdrawAccount;
    private String orderNo;

    private BigDecimal amount;

    private String bankCardInfoJson;

    private String bankCardInfo;//收款信息
    /**
     * 提现时间
     */
    private Timestamp createTime;
    /**
     * 关闭时间
     */
    private Timestamp closeTime;

    private String status;//状态

    private String dealAccount;//处理人账号
    private String remark;//备注
    /**
     * 付款凭证
     */
    private String paymentVoucher;
}

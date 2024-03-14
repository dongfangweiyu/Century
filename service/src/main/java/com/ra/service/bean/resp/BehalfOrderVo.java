package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 代付提现订单
 */
@Data
public class BehalfOrderVo {

    private Long id;
    private String withdrawAccount; //商户账号
    private String behalfAccount;//卡商账号
    private String orderNo;//系统订单号
    private String outOrderNo;//商户提现订单号
    private BigDecimal amount;

    private String bankCardInfoJson;//提现账户json信息

    private String bankCardInfo;//显示的收款信息
    private String behalfBankCardInfoJson;
    private String behalfBankCardInfo;//显示的付款信息

    private BigDecimal rateAmount;//提现的手续费

    private BigDecimal feeAmount;//提现的单笔费用
    /**
     * 提现时间
     */
    private Timestamp createTime;
    /**
     * 完成时间
     */
    private Timestamp completeTime;
    /**
     * 成功时间
     */
    private Timestamp successTime;
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

    private String realName;//代付订单收款银行卡持卡人姓名

    private String bankNo;

}

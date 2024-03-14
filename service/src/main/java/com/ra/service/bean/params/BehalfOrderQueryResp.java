package com.ra.service.bean.params;

import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BehalfOrderQueryResp implements Serializable {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 外部订单号
     */
    private String outOrderNo;
    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 备注
     */
    private String remark;
    /**
     * 金额
     */
    private BigDecimal amount;


    private Timestamp createTime;
    /**
     * 关闭时间
     */
    private Timestamp closeTime;

    /**
     * 回调完成时间
     */
    private Timestamp completeTime;

    /**
     * 支付成功时间，   确认收款时间
     */
    private Timestamp successTime;

    /**
     * 付款凭证
     */
    private String paymentVoucher;

    /**
     * 异步通知URL
     */
    private String notifyUrl;
    /**
     * 异步通知次数
     */
    private int notifyCount;

    /**
     * 附加数据
     */
    private String attach;

}

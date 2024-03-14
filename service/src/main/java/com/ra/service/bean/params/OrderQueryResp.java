package com.ra.service.bean.params;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class OrderQueryResp implements Serializable {

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
     * 支付方式
     */
    private String payCode;


    /**
     * 订单描述信息
     * 比如 转账付款，购买衣服充值，话费充值等...
     */
    private String orderDesc;

    /**
     * 附加数据
     */
    private String attach;

    /**
     * 异步通知次数
     */
    private int notifyCount;

    /**
     * 同步跳转URL
     */
    private String returnUrl;

    /**
     * 异步跳转URL
     */
    private String notifyUrl;


}

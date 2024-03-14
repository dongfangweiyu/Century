package com.ra.service.bean.params;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BehalfOrderListQueryResp implements Serializable {

    /**
     * 订单号
     */
    private String orderNo;

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
     * 收款银行卡信息
     */
    private String bankCardInfoJson;

    /**
     * 付款银行卡信息
     */
    private String behalfBankCardInfoJson;

}

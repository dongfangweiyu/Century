package com.ra.service.bean.req;

import lombok.Data;

@Data
public class BehalfOrderReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String status;//状态

    private String orderNo;//系统订单号
    private String outOrderNo;//商户提现订单号

    private String withdrawAccount; //商户账号

    private String dealAccount;//处理人账号

    private String behalfAccount;//卡商账号

    private String merchantBankNo;//收款客户的银行卡号

    private String behalfBankNo;//付款银行卡号

    private Long lastId;

    private String beginSuccessTime;//订单成功开始时间

    private String endSuccessTime;//订单成功结束时间
}

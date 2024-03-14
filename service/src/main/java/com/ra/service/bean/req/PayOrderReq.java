package com.ra.service.bean.req;

import lombok.Data;

/**
 * 接单订单列表查询条件
 */
@Data
public class PayOrderReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String merchantAccount;//商户账号

    private Long proxyUserId;//代理用户id

    private Long payChannelId;//通道名称ID

    private String status;//状态

    private String orderNo;//订单号

    private String outOrderNo;//商户订单号

    private Integer amount;

    private String payCode;//通道编码

}

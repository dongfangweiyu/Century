package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 订单列表
 */
@Data
public class PayOrderVo {
    private Long id;//订单ID

    private String orderNo;//订单号

    private String outOrderNo;//商户订单号

    private String merchantAccount;//商户账户

    private String companyName;//商户名称（方便查群）

    private BigDecimal amount;//接单金额

    private String payCode;//通道编码

    private String payChannelInfoJson;//匹配的通道信息Json

    private Timestamp createTime;//创建时间

    private Timestamp completeTime;//回调时间

    private Timestamp closeTime;//关闭时间

    private String status;//状态

    private String statusDesc;//回调信息状态

    private String errorMsg;//第三方返回的异常

    private String payChannelName;

    private String extraData;


}

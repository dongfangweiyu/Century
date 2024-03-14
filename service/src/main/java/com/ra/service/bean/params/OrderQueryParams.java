package com.ra.service.bean.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 创建OTC订单请求参数
 */
@Data
public class OrderQueryParams {

//    @NotBlank(message = "orderNo:订单号不能为空")
    private String orderNo;

//    @NotBlank(message = "outOrderNo:订单号不能为空")
    private String outOrderNo;

    @NotNull(message = "timestamp:请求时间戳不能为空,且时间差不能大于60秒")
    private Long timestamp;
    /**
     * 业务流水号，不小于10位字符的随机字符串
     */
    @NotEmpty(message = "nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性")
    private String nonceStr;

    @NotEmpty(message = "appId:不能为空")
    private String appId;

    @NotEmpty(message = "signature:签名不能为空")
    private String signature;

}

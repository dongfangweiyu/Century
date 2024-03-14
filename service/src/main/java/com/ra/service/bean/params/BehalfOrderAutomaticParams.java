package com.ra.service.bean.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 自动付款后自动助手请求参数
 */
@Data
public class BehalfOrderAutomaticParams {

    @NotNull(message = "amount:订单金额不能为空")
    private BigDecimal amount;
    @NotBlank(message = "orderNo:订单号不能为空")
    private String orderNo;
    @NotNull(message = "timestamp:请求时间戳不能为空,且时间差不能大于60秒")
    private Long timestamp;
    /**
     * 业务流水号，不小于10位字符的随机字符串
     */
    @NotBlank(message = "nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性")
    private String nonceStr;

    @NotBlank(message = "appId:不能为空")
    private String appId;

    @NotBlank(message = "signature:签名不能为空")
    private String signature;

    @NotBlank(message = "auditRequest:审核确认或驳回不能为空")
    private String auditRequest;//确认传confirm 驳回传reject

    private String remark;//驳回时传

}

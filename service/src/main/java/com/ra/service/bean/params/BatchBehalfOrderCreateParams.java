package com.ra.service.bean.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class BatchBehalfOrderCreateParams {
    /**
     * 银行名称
     * 建设银行
     */
    @NotBlank(message = "bankName:银行名称不能为空")
    private String bankName;

    /**
     * 卡号,该字段唯一
     * 62315....
     */
    @NotBlank(message = "bankNo:银行卡号不能为空")
    private String bankNo;

    /**
     * 持卡人姓名
     */
    @NotBlank(message = "realName:持卡人真实姓名不能为空")
    private String realName;

    @NotNull(message = "amount:订单金额不能为空")
    private BigDecimal amount;

    /**
     * 持卡人姓名
     */
    private String bankBranch;
}

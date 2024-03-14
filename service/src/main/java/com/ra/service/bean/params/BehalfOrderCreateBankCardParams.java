package com.ra.service.bean.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BehalfOrderCreateBankCardParams {
    /**
     * 银行名称
     * 建设银行
     */
    @NotBlank(message = "bankCard.bankNo:银行名称不能为空")
    private String bankName;

    /**
     * 卡号,该字段唯一
     * 62315....
     */
    @NotBlank(message = "bankCard.bankNo:银行卡号不能为空")
    private String bankNo;

    /**
     * 持卡人姓名
     */
    @NotBlank(message = "bankCard.bankNo:持卡人真实姓名不能为空")
    private String realName;

    /**
     * 支行信息
     */
    private String bankBranch;
}

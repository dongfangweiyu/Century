package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatchBehalfDataVo {

    private int index;

    private boolean isChecked;

    /**
     * 银行名称
     * 建设银行
     */
    private String bankName;

    /**
     * 卡号,该字段唯一
     * 62315....
     */
    private String bankNo;

    /**
     * 持卡人姓名
     */
    private String realName;

    private BigDecimal amount;

    private String bankBranch;

    /**
     * 匹配的银行卡通道
     */
    private BehalfBankCardResp behalfBankCardResp;
}

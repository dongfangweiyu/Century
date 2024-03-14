package com.ra.service.bean.resp;

import com.ra.dao.entity.business.BehalfBankCardGroupRelation;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardGroupRateVo extends BehalfBankCardGroupRelation {

    private int index;

    private boolean isChecked;
    /**
     * 卡组的名称
     */
    private String cardGroupName;
    /**
     *  卡商的下发费率
     */
    private BigDecimal behalfRate;

    /**
     *  卡商的单笔费用
     *
     */
    private BigDecimal behalfFee;
}

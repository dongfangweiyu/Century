package com.ra.service.bean.resp;

import com.ra.dao.entity.business.BehalfBankCard;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BehalfBankCardResp extends BehalfBankCard {

    /**
     * 代付对应的商户费率
     */
    private BigDecimal merchantRate;

    /**
     * 代付对应的代理费率
     */
    private BigDecimal proxyRate;

    /**
     * 商户单笔扣除
     */
    private BigDecimal merchantFee;
}

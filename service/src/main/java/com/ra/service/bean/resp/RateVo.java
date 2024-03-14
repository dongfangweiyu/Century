package com.ra.service.bean.resp;

import com.ra.dao.entity.business.Rate;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateVo extends Rate {

    private int index;

    private boolean isChecked;
    /**
     * 通道的名称
     */
    private String payChannelName;
    /**
     * t第三方通道的费率
     */
    private BigDecimal channelRate;
}

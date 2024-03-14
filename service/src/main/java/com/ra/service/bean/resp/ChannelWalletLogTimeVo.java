package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 通道瞬时额度
 */
@Data
public class ChannelWalletLogTimeVo {


    private Long configPayInterfaceId;

    private String configName;//通道名称

    private BigDecimal balance;//变动后的余额

    private Timestamp createTime;
}

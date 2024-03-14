package com.ra.service.bean.resp;

import com.ra.dao.Enum.WalletLogEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class WalletLogTimeVo {
    private String account;//账户

    private BigDecimal balance;//变动后的余额

    private Timestamp createTime;

}

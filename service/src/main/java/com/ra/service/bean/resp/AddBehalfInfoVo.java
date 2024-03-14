package com.ra.service.bean.resp;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 添加卡商的信息及详情
 */
@Data
public class AddBehalfInfoVo {


    private Long userId;
    private String behalfAccount;
    private String password;
    /**
     * 公司名称
     */
    private String companyName;

    private BigDecimal behalfRate;

    /**
     * 单笔扣除
     */
    private BigDecimal behalfFee;


}

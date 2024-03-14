package com.ra.service.bean.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 添加商户的信息及详情
 */
@Data
public class AddMechantInfoVo {


    private Long userId;
    private String merchantAccount;
    private String password;
    /**
     * 公司名称
     */
    private String companyName;

    private Long proxyUserId;

    private List<RateVo> rateVoList;


}

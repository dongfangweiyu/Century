package com.ra.service.bean.resp;


import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;


@Data
public class MechantInfoListVo {
    /**
     * 商户序列
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 商户账户
     */
    private String account;

    /**
     * 代理商
     */
    private String proxyName;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 公司地址
     */
    private String companyAddress;

    /**
     *  商户费率
     */
    private BigDecimal wxRate;
    private BigDecimal alipayRate;
    private BigDecimal cloudRate;
    private BigDecimal pddRate;

    /**
     * 抽取代理商分红
     */
    private BigDecimal proxyRate;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 状态
     */
    private Integer status;

    //商户钱包总额
    private BigDecimal money;

}

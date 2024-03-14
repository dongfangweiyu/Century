package com.ra.dao.entity.business;

import com.ra.dao.Enum.IncomeEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 收入记录
 */
@Data
@Entity
@Table(name="business_income_log")
public class IncomeLog extends BaseEntity {

    /**
     * 收入用户
     * 如果为空，就是平台收益
     */
    @org.hibernate.annotations.Index(name = "INDEX_incomeUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long incomeUserId;

    /**
     * 来源的订单号
     */
    private String fromOrderNo;

    /**
     * 订单金额
     */
    @Column(columnDefinition = "decimal(19,2) COMMENT '订单金额'")
    private BigDecimal fromOrderAmount;
    /**
     * 费率
     */
    @Column(columnDefinition = "decimal(19,5) COMMENT '费率'")
    private BigDecimal rate;
    /**
     * 收入金额
     */
    @Column(columnDefinition = "decimal(19,5) COMMENT '收入金额'")
    private BigDecimal incomeMoney;
    /**
     * 描述
     */
    private String description;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Index(name = "INDEX_incomeEnum")//该注解来自Hibernate包 不能使用java.persistence包
    @Column(columnDefinition = " varchar(13) ")
    private IncomeEnum incomeEnum;//收入类型
}

package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 卡商利润变动日志
 */
@Data
@Entity
@Table(name="business_behalf_profit_log")
public class BehalfProfitLog extends BaseEntity {

    /**
     * 变动的金额
     * 正数为增加
     * 负数为扣减
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal  amount;

    /**
     * 用户id
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_behalfUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long behalfUserId;

    /**
     * 变动前余额
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal beforeBalance;
    /**
     * 变动后余额
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal balance;
    /**
     * 描述说明
     */
    @Column(nullable = false)
    private String description;

    /**
     * 变动方式 rollIn=转入 rollOut=转出
     */
    @Column(nullable =false)
    private String way;

}

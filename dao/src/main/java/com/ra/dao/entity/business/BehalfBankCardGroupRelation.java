package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * 提现代付银行卡与商户的关系表
 */
@Data
@Entity
@Table(name="business_behalf_bankCardGroupRelation",uniqueConstraints = {@UniqueConstraint(columnNames={"merchantUserId","bankCardGroupId"})})
public class BehalfBankCardGroupRelation extends BaseEntity {

    /**
     * 商户的userId
     */
    @Column(name = "merchantUserId",nullable = false)
    private Long merchantUserId;

    /**
     * 代付银行卡组id
     */
    @Column(name = "bankCardGroupId",nullable = false)
    private Long bankCardGroupId;


    /**
     * 代付对应的商户费率
     */
    @Column(columnDefinition = " decimal(18,4) COMMENT '商户费率'")
    private BigDecimal merchantRate;

    /**
     * 代付对应的代理费率
     */
    @Column(columnDefinition = "decimal(18,4) COMMENT '代理费率'")
    private BigDecimal proxyRate;

    /**
     * 商户单笔扣除
     */
    @Column(nullable = false,columnDefinition = "decimal(18,2) COMMENT '商户单笔扣除'")
    private BigDecimal merchantFee;

    /**
     * 代理单笔扣除
     */
    @Column(nullable = false,columnDefinition = "decimal(18,2) COMMENT '代理单笔扣除'")
    private BigDecimal proxyFee;

}

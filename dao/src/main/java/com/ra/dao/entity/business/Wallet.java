package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 钱包
 */
@Data
@Entity
@Table(name="business_wallet")
public class Wallet extends BaseEntity {


    /**
     * 关联用户id
     */
    @Column(name = "userId", columnDefinition = "bigint(20) COMMENT '关联用户Id'",unique = true)
    private Long userId;

    /**
     * 钱包总额，包含冻结金额，不可用金额等
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal money=BigDecimal.ZERO;

}

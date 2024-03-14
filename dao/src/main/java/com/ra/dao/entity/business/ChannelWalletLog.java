package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 通道钱包变动日志
 */
@Data
@Entity
@Table(name="business_channel_wallet_log")
public class ChannelWalletLog extends BaseEntity {

    /**
     * 变动的金额
     * 正数为增加
     * 负数为扣减
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal  amount;

    /**
     * 接口配置ID
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_configPayInterfaceId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long configPayInterfaceId;

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
}

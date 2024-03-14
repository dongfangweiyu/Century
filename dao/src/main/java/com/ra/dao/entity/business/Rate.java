package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * 费率表
 */
@Data
@Entity
@Table(name="business_rate",uniqueConstraints = {@UniqueConstraint(columnNames={"merchantInfoId","payChannelId"})})
public class Rate extends BaseEntity {


    /**
     * 绑定的商户的merchantInfoId
     */
    private Long merchantInfoId;

    /**
     * 绑定的通道id
     */
    private Long payChannelId;

    /**
     * 对应的商户费率
     */
    @Column(columnDefinition = "decimal(19,4) COMMENT '商户费率'")
    private BigDecimal merchantRate;

    /**
     * 对应的代理费率
     */
    @Column(columnDefinition = "decimal(19,4) COMMENT '代理费率'")
    private BigDecimal proxyRate;


    @Column(columnDefinition = "int(3) default 100 COMMENT '权重'")
    private Integer weight;
}

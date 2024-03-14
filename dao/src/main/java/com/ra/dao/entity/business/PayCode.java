package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 支付类型（支付编码）
 */
@Data
@Entity
@Table(name="business_payCode")
public class PayCode extends BaseEntity {

    /**
     * 类型名称
     */
    private String name;

    /**
     * 编码
     */
    @Column(unique = true,nullable = false,columnDefinition = " varchar(15) ")
    private String code;

    /**
     * 是否启用，0不启用，1启用
     */
    private boolean isEnable;
}

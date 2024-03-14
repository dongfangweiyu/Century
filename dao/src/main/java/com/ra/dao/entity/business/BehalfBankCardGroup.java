package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 提现代付银行卡组
 */
@Data
@Entity
@Table(name="business_behalf_bankCardGroup")
public class BehalfBankCardGroup extends BaseEntity {

    /**
     * 代付商userID
     */
    @Column(name = "behalfUserId",nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_behalfUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long behalfUserId;

    /**
     * 卡组名称
     */
    @Column(name = "cardGroupName",nullable = false)
    private String cardGroupName;

    /**
     * 是否启用，默认true，0不启用，1启用
     */
    private boolean enable;

}

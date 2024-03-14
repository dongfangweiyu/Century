package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 商户下发提现银行卡
 */
@Data
@Entity
@Table(name="business_bankCard")
public class BankCard extends BaseEntity {

    /**
     * 哪个user绑定的银行卡
     */
    @org.hibernate.annotations.Index(name = "INDEX_userId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long userId;
    /**
     * 银行名称
     * 建设银行
     */
    private String bankName;

    /**
     * 卡号
     * 62315....
     */
    private String bankNo;

    /**
     * 持卡人姓名
     */
    private String realName;

    /**
     * 支行信息
     */
    private String bankBranch;

    /**
     * 是否启用，0不启用，1启用
     */
    private boolean isEnable;


}

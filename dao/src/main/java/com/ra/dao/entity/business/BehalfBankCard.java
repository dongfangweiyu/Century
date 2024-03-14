package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 提现代付银行卡
 */
@Data
@Entity
@Table(name="business_behalf_bankCard")
public class BehalfBankCard extends BaseEntity {


    /**
     * 分组id
     */
    @Column(name = "behalfGroupId",nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_behalfGroupId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long behalfGroupId;

    /**
     * 代付商userID
     */
    @Column(name = "behalfUserId",nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_behalfUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long behalfUserId;
    /**
     * 银行名称
     * 建设银行
     */
    @Column(name = "bankName",nullable = false,columnDefinition = " varchar(20) ")
    private String bankName;

    /**
     * 卡号,该字段唯一
     * 62315....
     */
    @Column(name = "bankNo",nullable = false,unique = true,columnDefinition = " varchar(30) ")
    private String bankNo;

    /**
     * 持卡人姓名
     */
    @Column(name = "realName",nullable = false,columnDefinition = " varchar(5) ")
    private String realName;

    /**
     * 支行信息
     */
    private String bankBranch;

    /**
     * 余额
     */
    @Column(name ="balance",nullable = false,columnDefinition = "decimal(19,2)")
    private BigDecimal balance=BigDecimal.ZERO;

    /**
     * 是否默认打进，默认false,0不启用，1启用
     */
    private boolean comeIn=false;

    /**
     * 是否匹配打出，默认true，0不启用，1启用
     */
    private boolean comeOut=true;

    /**
     * 备注
     */
    @Lob
    private String remark;




}

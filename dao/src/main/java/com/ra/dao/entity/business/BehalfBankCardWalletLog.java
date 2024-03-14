package com.ra.dao.entity.business;

import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 代付银行卡金额变动日志
 */
@Data
@Entity
@Table(name="business_behalf_bankcard_wallet_log")
public class BehalfBankCardWalletLog extends BaseEntity {

    /**
     * 变动的金额
     * 正数为增加
     * 负数为扣减
     */
    @Column(nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal  amount;
    /**
     * 卡号
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_bankNo")//该注解来自Hibernate包 不能使用java.persistence包
    private String bankNo;

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

    /**
     * 变动类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable =false)
    private BehalfWalletLogEnum logEnum;
}

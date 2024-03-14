package com.ra.dao.entity.business;

import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 提现订单表
 */
@Data
@Entity
@Table(name="business_order_withdraw")
public class WithdrawlOrder extends BaseEntity {

    /**
     * 订单号
     */
    @Column(unique = true,nullable = false,columnDefinition = " varchar(30) ")
    private String orderNo;


    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Index(name = "INDEX_status")//该注解来自Hibernate包 不能使用java.persistence包
    @Column(columnDefinition = " varchar(10) ")
    private OrderStatusEnum status;

    /**
     * 金额
     */
    @Column(name = "amount", columnDefinition = "decimal(19) COMMENT '金额，整数型'",nullable = false)
    private BigDecimal amount;

    /**
     * 下单的商户用户id
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_merchantUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long merchantUserId;

    /**
     * 关闭时间
     */
    private Timestamp closeTime;

    /**
     * 完成时间
     */
    private Timestamp completeTime;


    /**
     * 银行卡信息Json
     */
    @Lob
    private String bankCardInfoJson;

    /**
     * 处理人账号
     */
    private String dealAccount;
    /**
     * 订单备注
     */
    private String remark;

    /**
     * 付款凭证
     */
    private String paymentVoucher;

    /**
     * 生成订单号
     * @Param prefix 前缀
     * @return
     */
    public static String genaralOrderNo(String prefix){
        return prefix+ NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4);
    }
}

package com.ra.dao.entity.business;

import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 代付订单表
 */
@Data
@Entity
@Table(name="business_behalf_order")
public class BehalfOrder extends BaseEntity {

    /**
     * 系统订单号
     */
    @Column(unique = true,nullable = false,columnDefinition = " varchar(30) ")
    private String orderNo;

    /**
     * 商户订单号
     */
    @Column(nullable = false,columnDefinition = " varchar(30) ")
    @org.hibernate.annotations.Index(name = "INDEX_outOrderNo")//该注解来自Hibernate包 不能使用java.persistence包
    private String outOrderNo;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Index(name = "INDEX_status")//该注解来自Hibernate包 不能使用java.persistence包
    @Column(columnDefinition = " varchar(10) ")
    private OrderStatusEnum status;

    /**
     * 状态描述
     */
    @Column(name="statusDesc",columnDefinition=" TEXT COMMENT '状态描述'")
    private String statusDesc;

    /**
     * 金额
     */
    @Column(name = "amount", columnDefinition = "decimal(19,2) COMMENT '金额，整数型'",nullable = false)
    private BigDecimal amount;

    /**
     * 手续费金额
     */
    @Column(name = "rateAmount", columnDefinition = "decimal(19,5) COMMENT '手续费'",nullable = false)
    private BigDecimal rateAmount;

    /**
     * 单笔费用
     */
    @Column(name = "feeAmount", columnDefinition = "decimal(19,2) COMMENT '手续费'",nullable = false)
    private BigDecimal feeAmount;

    /**
     * 下单的商户用户id
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_merchantUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long merchantUserId;

    /**
     * 代付商用户id
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_behalfUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long behalfUserId;

    /**
     * 关闭时间
     */
    private Timestamp closeTime;

    /**
     * 回调完成时间
     */
    private Timestamp completeTime;

    /**
     * 订单成功时间
     */
    private Timestamp successTime;

    /**
     * 银行卡信息Json
     */
    @Lob
    private String bankCardInfoJson;

    /**
     * 付款银行卡信息Json
     */
    @Lob
    private String behalfBankCardInfoJson;

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
     * 异步跳转URL
     */
    @Column(nullable = false)
    private String notifyUrl;
    /**
     * 异步通知次数
     */
    private int notifyCount;

    /**
     * 附加数据
     */
    private String attach;

    /**
     * 收款银行卡
     */
    private String merchantBankNo;

    /**
     * 付款银行卡
     */
    private String behalfBankNo;

    /**
     * 生成订单号
     * @Param prefix 前缀
     * @return
     */
    public static String genaralOrderNo(String prefix){
        return prefix+ NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4);
    }
}

package com.ra.dao.entity.business;

import com.alibaba.fastjson.JSON;
import com.ra.common.bean.ExtraData;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.base.BaseEntity;
import com.ra.dao.factory.ConfigFactory;
import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 支付订单表
 */
@Data
@Entity
@Table(name="business_order_pay")
public class PayOrder extends BaseEntity {

    /**
     * 订单号
     */
    @Column(unique = true,nullable = false,columnDefinition = " varchar(30) ")
    private String orderNo;

    /**
     * 外部订单号
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
    @Column(name="statusDesc",columnDefinition="TEXT COMMENT '状态描述'")
    private String statusDesc;

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
     * 下单的商户账号
     */
    @Column(nullable = false,columnDefinition = " varchar(25) ")
    @org.hibernate.annotations.Index(name = "INDEX_merchantAccount")//该注解来自Hibernate包 不能使用java.persistence包
    private String merchantAccount;
    /**
     * 下单匹配的通道id
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Index(name = "INDEX_payChannelId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long payChannelId;

    /**
     * 关闭时间
     */
    private Timestamp closeTime;

    /**
     * 回调完成时间
     */
    private Timestamp completeTime;

    /**
     * 支付成功时间，   确认收款时间
     */
    private Timestamp successTime;

    /**
     * 支付编码
     */
//    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Index(name = "INDEX_payCode")//该注解来自Hibernate包 不能使用java.persistence包
    @Column(columnDefinition = " varchar(15) ")
    private String payCode;

    /**
     * 匹配的通道信息Json
     */
    @Lob
    private String payChannelInfoJson;

    /**
     * 匹配的通道的费率信息Json
     */
    @Lob
    private String rateInfoJson;

    /**
     * 额外数据
     */
    @Column(name = "extraData",columnDefinition = " text ")
//    @Type(type = "com.ra.common.domain.ExtraDataUserType")
    private String extraData;

    /**
     * 订单描述信息
     * 比如 转账付款，购买衣服充值，话费充值等...
     */
    private String orderDesc;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 商户附加数据
     */
    private String attach;

    /**
     * 异步通知次数
     */
    private int notifyCount;

    /**
     * 同步跳转URL
     */
    private String returnUrl;

    /**
     * 异步跳转URL
     */
    @Column(nullable = false)
    private String notifyUrl;

    /**
     * 下单时，第三方返回的错误信息
     */
    @Lob
    private String errorMsg="正常";
    /**
     * 生成订单号
     * @Param prefix 前缀
     * @return
     */
    public static String genaralOrderNo(String prefix){
        return prefix+ NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4);
    }

    /**
     * 生成测试对象
     * @param amount
     * @return
     */
    public static PayOrder test(BigDecimal amount){
        PayOrder payOrder=new PayOrder();
        payOrder.setOrderNo(PayOrder.genaralOrderNo(ConfigFactory.get(ConfigEnum.ORDER_PREFIX)));
        payOrder.setOutOrderNo(NumberUtil.generateCharacterString(11));
        payOrder.setAmount(amount);
        payOrder.setAttach("");
        payOrder.setMerchantUserId(null);
        payOrder.setStatus(OrderStatusEnum.PROCESS);
        payOrder.setPayCode(null);
        payOrder.setStatusDesc("新订单生成,等待支付");
        payOrder.setNotifyCount(0);
        payOrder.setNotifyUrl("");
        payOrder.setReturnUrl("");
        payOrder.setOrderDesc("测试一下");
        payOrder.setPayChannelId(null);
        payOrder.setPayChannelInfoJson("");
        payOrder.setMerchantAccount("测试一下");
        return payOrder;
    }
}

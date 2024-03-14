package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import com.ra.dao.channel.PayInterfaceEnum;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 支付通道表
 */
@Data
@Entity
@Table(name="business_payChannel")
public class PayChannel extends BaseEntity {

    /**
     * 通道名称
     */
    private String channelName;

    /**
     * 该通道绑定的支付接口
     * 支付接口由技术对接后，于代码中添加
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PayInterfaceEnum payInterface;

    /**
     * 相应的支付接口的类型
     * 按第三方给出的标准
     */
    private String payInterfaceType;

    @Column(nullable = false,columnDefinition = "bigint(20) COMMENT '绑定的配置表的id' ")
    private Long configPayInterfaceId;

    /**
     * 第三方通道官网
     */
    private String channelHost;

    /**
     * 第三方的费率
     */
    @Column(nullable = false,columnDefinition = "decimal(19,3) COMMENT '第三方费率'")
    private BigDecimal rate;

    /**
     * 风控开关
     * 默认不开启风控
     */
    private boolean risk=false;


    /**
     * 通道的信誉分
     * 每下一单，信誉分相应扣除，如果信誉分不够，将不能再匹配到该通道去创建订单
     */
    private BigDecimal creditScore=BigDecimal.ZERO;

    /**
     * 订单最小金额
     */
    private BigDecimal minAmount;

    /**
     * 订单最大金额
     */
    private BigDecimal maxAmount;

    /**
     * 交易开始时间
     */
    private String beginTime;

    /**
     * 交易结束时间
     */
    private String endTime;

    /**
     * 是否启用，0不启用，1启用
     */
    private boolean isEnable;

    /**
     * 生成测试对象
     * @return
     */
    public static PayChannel test(PayInterfaceEnum payInterfaceEnum,String payInterfaceType,long configPayInterfaceId){
        PayChannel payChannel=new PayChannel();
        payChannel.setPayInterface(payInterfaceEnum);
        payChannel.setPayInterfaceType(payInterfaceType);
        payChannel.setConfigPayInterfaceId(configPayInterfaceId);
        return payChannel;
    }
}

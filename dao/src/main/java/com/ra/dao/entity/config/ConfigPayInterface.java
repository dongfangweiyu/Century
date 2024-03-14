package com.ra.dao.entity.config;

import com.ra.dao.base.BaseEntity;
import com.ra.dao.channel.PayInterfaceEnum;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 接口配置
 */
@Data
@Entity
@Table(name="config_payInterface")
public class ConfigPayInterface extends BaseEntity {

    /**
     * 接口类型
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PayInterfaceEnum payInterfaceEnum;

    /**
     * 配置名称
     */
    @Column(nullable = false)
    private String configName;
    /**
     * 创建订单的接口地址
     */
    @Column(nullable = false)
    private String createOrderUrl;

    /**
     * 第三方APPID
     */
    @Column(nullable = false)
    private String appId;

    /**
     * 第三方秘钥
     */
    @Column(nullable = false)
    private String secret;

    /**
     * 绑定IP
     * 多个IP以,分割开
     */
    @Lob
    @Column(name="bindIP",columnDefinition="TEXT COMMENT ' 绑定IP，多个IP以,分割开'")
    private String bindIP;

    /**
     * 自定义参数json
     */
    private String json;

    /**
     * 通道余额；
     * 每成交一笔或手动修改
     * updatable=false,设置为这个字段不随着save()方法而更改
     */
    @Column(updatable=false,nullable = false,columnDefinition = "decimal(19,5)")
    private BigDecimal money=BigDecimal.ZERO;

    /**
     * 瞬时余额限制，当通道余额“大于”这个瞬时余额限制不给回调，给通道下发后才可回调成功
     */
    @Column(name="balanceLimit",columnDefinition = "decimal(19,5)")
    private BigDecimal balanceLimit=BigDecimal.ZERO;

}

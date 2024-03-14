package com.ra.dao.entity.business;

import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 商户信息
 */
@Data
@Entity
@Table(name="business_merchant_info")
public class MerchantInfo extends BaseEntity {

    /**
     * 属于哪个用户
     */
    @Column(unique = true,nullable = false)
    private Long userId;

    /**
     * 属于哪个代理的商户
     * 业务员代理的userId
     */
    @Column(name = "proxyUserId",nullable = true)
    @org.hibernate.annotations.Index(name = "INDEX_proxyUserId")//该注解来自Hibernate包 不能使用java.persistence包
    private Long proxyUserId;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * appId
     * 商户编号
     */
    @Column(unique = true,nullable = false)
    private String appId;


    /**
     * 秘钥
     */
    private String secret;


    /**
     * 代理人申请时审核状态
     *   NOTPASS 未通过
     *   PASS 通过
     *   INAUDIT 审核中
     */
    @Enumerated(EnumType.STRING)
    private ApplyMerchantEnum applyStatus;

    /**
     * 备注
     */
    private String applyDesc;

    /**
     * 绑定IP
     * 多个IP以,分割开
     */
    @Lob
    @Column(name="behalfIp",columnDefinition="TEXT COMMENT ' 绑定IP，多个IP以,分割开'")
    private String behalfIp;//代付下单ID验证
    /**
     * 生成APPID
     * @return
     */
    public String generationAppId(){
        return NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(3);
    }

    /**
     * 生成秘钥
     */
    public String generationSecret(){
        Assert.hasText(getAppId(),"appid must not empty...");
        return EncryptUtil.encodeSHA(getAppId()+":secret:"+getId());
    }

}

package com.ra.dao.entity.business;

import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.base.BaseEntity;
import io.netty.util.internal.StringUtil;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 代付商信息表
 */
@Data
@Entity
@Table(name="business_behalf_info")
public class BehalfInfo extends BaseEntity {

    /**
     * 属于哪个用户
     */
    @Column(unique = true,nullable = false)
    private Long userId;

    /**
     * 公司名称
     */
    @Column(nullable = false)
    private String companyName;

    /**
     * 百分比费率
     */
    @Column(nullable = false,columnDefinition = "decimal(18,5) COMMENT '百分比费率'")
    private BigDecimal behalfRate;

    /**
     * 单笔扣除
     */
    @Column(nullable = false,columnDefinition = "decimal(18,2) COMMENT '单笔扣除'")
    private BigDecimal behalfFee;

    @ColumnDefault(value = "0")
    @Column(nullable = false,columnDefinition = "decimal(18,5) COMMENT '利润'")
    private BigDecimal profit;//利润

    /**
     * appId
     * 卡商编号
     */
    @Column(unique = true,nullable = false)
    private String appId;


    /**
     * 秘钥
     */
    private String secret;

    /**
     * 绑定IP
     * 多个IP以,分割开
     */
    @Lob
    @Column(name="behalfIp",columnDefinition="TEXT COMMENT ' 绑定IP，多个IP以,分割开'")
    private String behalfIp;//代付订单自动确认IP验证

    /**
     * 生成APPID
     * @return
     */
    public String generationAppId(){
        return NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4);
    }

    /**
     * 生成秘钥
     */
    public String generationSecret(){
        Assert.hasText(getAppId(),"appid must not empty...");
        return EncryptUtil.encodeSHA(getAppId()+":secret:"+getId());
    }

}

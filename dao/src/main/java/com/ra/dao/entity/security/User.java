package com.ra.dao.entity.security;

import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 用户实体
 *
 * @author Administrator
 */
@Data
@Entity
@Table(name = "security_user")
public class User extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    /**
     * 账号
     */
    @Column(name = "account", columnDefinition = "varchar(25) COMMENT '账号'", unique = true,nullable = false)
    private String account;

    /**
     * 头像
     */
    @Column(name = "avatar", columnDefinition = "varchar(200) COMMENT '头像'")
    private String avatar;

    /**
     * 密码，采用先MD5再AES加密
     */
    @Column(name = "password", columnDefinition = "varchar(255) COMMENT '密码'",nullable = false)
    private String password;

    /**
     * 支付密码 MD5再AES加密
     */
    @Column(name = "pay_password", columnDefinition = "varchar(255) COMMENT '支付密码'")
    private String pay_password;

    /**
     * 账号权限，管理员/领导人/业务员(代理)/商户
     */
    @Enumerated(EnumType.STRING)
    private AgencyEnum agencyType;

    /**
     * 角色id
     */
    @Column(name = "roleID", columnDefinition = "int(5) COMMENT '角色id'")
    private Integer roleID;

    /**
     * 状态 0 正常 1 禁用
     */
    @Column(name =  "status", columnDefinition = "int(5) COMMENT '状态'")
    private Integer status;

    /**
     * 登陆ip
     */
    @Column(name = "loginIp", columnDefinition = "varchar(20) COMMENT '登陆ip'")
    private String loginIp;

    /**
     * 最后登陆时间
     */
//    @Column(name = "lastLoginTime", columnDefinition = "TIMESTAMP COMMENT '最后登陆时间'")
    private Timestamp lastLoginTime;

    @Column(name="remark",columnDefinition="TEXT COMMENT '备注'")
    private String remark;

    /**
     * 谷歌二次验证
     */
    @Column(name = "googleAuthenticator",columnDefinition = "varchar(55) COMMENT '谷歌二次验证秘钥'")
    private String googleAuthenticator;

    /**
     * 生成用户token
     * @return
     */
    public static String generalToken(){
        return "TOKEN-"+ NumberUtil.UUID().toUpperCase()+"-"+ NumberUtil.generateCharacterString(4).toUpperCase();
    }

}

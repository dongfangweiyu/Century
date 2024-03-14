package com.ra.dao.Enum;

import lombok.Getter;

/**
 * 系统配置项的key,枚举
 */
public enum ConfigEnum {
    //系统相关配置项
    SYSTEM_ENABLE("false","系统维护,true 维护，false正常"),
    SYSTEM_CREATEORDER("true","放单通道开关，true开，false关"),
    SYSTEM_CREATEBEHALFORDER("true","代付通道开关，true开，false关"),
    SYSTEM_WEBNAME("layuiAdmin","网站名称"),
    SYSTEM_WEBDOMAIN("http://www.layui.com","网站域名"),
    SYSTEM_WEBTITLE("layuiAdmin std - 通用后台管理模板系统（iframe标准版）","网站标题"),
    SYSTEM_WEBCOPYRIGHT("© 2018 layui.com MIT license","版权信息"),
    SYSTEM_DEMO("true","是否开启演示demo,true开启，false关闭"),
    //订单相关配置项
    ORDER_PREFIX("","订单号前缀"),

    //提现相关
    WITHDRAW_RATE("0","提现的单笔手续费（元）");

    @Getter
    private String defaultValue;

    @Getter
    private String desc;

    ConfigEnum(String defaultValue,String desc){
        this.defaultValue=defaultValue;
        this.desc=desc;
    }
}

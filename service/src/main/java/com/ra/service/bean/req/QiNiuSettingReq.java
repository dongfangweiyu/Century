package com.ra.service.bean.req;


import lombok.Data;

/**
 * 佣金设置
 */
@Data
public class QiNiuSettingReq {

    private OSSEnum ossMethod;//存储方式，LOCAL本地， QINIU 七牛

    private String accessKey;//七牛key

    private String secretKey;//七牛秘钥

    private String bucket;//七牛空间名

    private String host;//下载域


    public enum OSSEnum{

        LOCAL,
        QINIU

    }
}

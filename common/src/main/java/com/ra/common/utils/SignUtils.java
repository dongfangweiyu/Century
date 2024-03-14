package com.ra.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

/**
 * @description 生成签名的工具类
 * Created by EDZ on 2018/9/29.
 */
public class SignUtils {

    private static final Logger logger= LoggerFactory.getLogger(SignUtils.class);
    /**
     * 查询订单签名
     * @description 生成参数签名
     * @Param appId 商户的APPID
     * @Param timestamp 13位时间戳
     * @Param nonceStr 业务流水随机字符串
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    public static String generateQuerySign(String appId,Long timestamp,String nonceStr, String secret) {
        String md5Value = EncryptUtil.encodeMD5( appId + timestamp + nonceStr).toLowerCase();
        String orginSignature = EncryptUtil.encodeMD5(md5Value + secret).toUpperCase();
        return orginSignature;
    }

    /**
     * 卡商自动确认签名验证
     * @param orderNo 代付订单号
     * @param amount 代付金额
     * @param appId 卡商编号
     * @param timestamp 时间戳
     * @param nonceStr 业务流水随机字符串
     * @param secret 卡商密钥
     * @return
     */
    public static String generateAutomaticConfirmSign(String orderNo,BigDecimal amount,String appId,Long timestamp,String nonceStr, String secret) {
        Map<String,String> params=new HashMap<>();
        params.put("amount",amount.toPlainString());
        params.put("appId",appId);
        params.put("nonceStr",nonceStr);
        params.put("orderNo",orderNo);
        params.put("timestamp",timestamp.toString());

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
            requestUrl.append(key).append("=");
            try{
                requestUrl.append(URLEncoder.encode(params.get(key), "UTF-8"));
            }catch (UnsupportedEncodingException e){
                requestUrl.append(params.get(key));
            }
            requestUrl.append("&");
        }

        String requestParamsEncode= requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
        logger.info(requestParamsEncode);
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        logger.info(md5Value);
        String orginSignature = EncryptUtil.encodeMD5(md5Value + secret).toUpperCase();
        logger.info(orginSignature);
        return orginSignature;
    }

    /**
     * 创建订单签名
     * @description 生成参数签名
     * @Param outOrderNo 商户订单号
     * @Param amount 金额
     * @Param payType 支付通道
     * @Param attach 订单附加参数
     * @Param appId 商户的APPID
     * @Param timestamp 13位时间戳
     * @Param nonceStr 业务流水随机字符串
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    public static String generateCreateSign(String outOrderNo, BigDecimal amount,
                                      String payCode, String attach,
                                      String appId, Long timestamp, String nonceStr, String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("amount",amount.toPlainString());
        params.put("appId",appId);
        params.put("attach",attach);
        params.put("nonceStr",nonceStr);
        params.put("outOrderNo",outOrderNo);
        if(!StringUtils.isEmpty(payCode)){
            //这个if判断是为了兼容，支付订单签名跟代付订单签名的payCode字段问题
            params.put("payCode",payCode);
        }
        params.put("timestamp",timestamp.toString());

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
            requestUrl.append(key).append("=");
            try{
                requestUrl.append(URLEncoder.encode(params.get(key), "UTF-8"));
            }catch (UnsupportedEncodingException e){
                requestUrl.append(params.get(key));
            }
            requestUrl.append("&");
        }

        String requestParamsEncode= requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
        logger.info(requestParamsEncode);
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        logger.info(md5Value);
        String orginSignature = EncryptUtil.encodeMD5(md5Value + secret).toUpperCase();
        logger.info(orginSignature);
        return orginSignature;
    }
}

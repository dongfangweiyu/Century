package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

/**
 * KKone 接口
 * 与第三方对接的具体实现
 */
public class KKOneInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String userUnqueNo= NumberUtil.generateCharacterString(6);
        String secret=config.getSecret();

        String nonceStr= NumberUtil.generateCharacterString(18).toUpperCase();

        Long timestamp=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("amount", payOrder.getAmount());// 金额
        params.put("attach", payOrder.getOrderNo());// 自定义参数
        params.put("payType", payChannel.getPayInterfaceType());// 支付方式
        params.put("userUnqueNo", userUnqueNo);// 用户唯一编号
        params.put("orderDesc", payOrder.getOrderDesc());// 商品信息描述
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("returnUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("appId", appId);// 商户ID
        params.put("outOrderNo", payOrder.getOrderNo());
        params.put("timestamp", timestamp);// 时间戳
        params.put("nonceStr",nonceStr);//业务流水号，18位长度以上的随机字符串

        params.put("signature", generateCreateSign(payOrder.getOrderNo(),params.get("amount").toString(),payChannel.getPayInterfaceType(),payOrder.getOrderNo()
                ,appId, timestamp.toString(),nonceStr, secret));// 签名
        logger.info("kkone下单参数："+params);
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==0){
                return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonObject.getString("data"));
            }
            return new ApiResult().fail(jsonObject.getString("msg"));
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");
        String orderNo=request.getParameter("orderNo");
        String outOrderNo=request.getParameter("outOrderNo");
        String signature=request.getParameter("signature");
        String payType=request.getParameter("payType");
        String amount=request.getParameter("amount");
        String nonceStr=request.getParameter("nonceStr");
        String status=request.getParameter("status");
        String timestamp=request.getParameter("timestamp");
        String attach=request.getParameter("attach");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(outOrderNo);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)))!=0){
            return "金额不对";
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "paychannel is null";
        }

        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        if(config==null){
            return "config is null";
        }
        boolean b= verifyIP(config,request);
        if(!b){
            return "回调IP不正确";
        }

        String appId=config.getAppId();
        String serect=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String sign=notifySign(outOrderNo,amount,payType,attach,appId, timestamp,nonceStr,serect,status);
            if (signature.equals(sign)) {
                if(status.equals("SUCCESS")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return "SUCCESS";
                    }
                    return "订单已完成,请结束回调";
                }
                return "支付失败";
            }
            return "验签失败";
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        }
        return "FAIL";
    }

    @Override
    public String parseData(Model model,PayOrder payOrder, String jsonData) throws Exception{
        return "order";
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
    private static String generateCreateSign(String outOrderNo, String amount,
                                            String payType, String attach,
                                            String appId, String timestamp, String nonceStr, String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("outOrderNo",outOrderNo);
        params.put("amount",amount);
        params.put("payType",payType);
        params.put("appId",appId);
        params.put("timestamp",timestamp);
        params.put("attach",attach);
        params.put("nonceStr",nonceStr);
        params.put("secret",secret);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder("?");
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
        logger.info("KKone签名原串："+requestParamsEncode);
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        logger.info("KKone签名结果："+md5Value);
        return md5Value;
    }

    /**
     * 回调订单签名
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
    private static String notifySign(String outOrderNo, String amount,
                                             String payType, String attach,
                                             String appId, String timestamp, String nonceStr, String secret,String status) {

        Map<String,String> params=new HashMap<>();
        params.put("outOrderNo",outOrderNo);
        params.put("amount",amount);
        params.put("payType",payType);
        params.put("appId",appId);
        params.put("timestamp",timestamp);
        params.put("attach",attach);
        params.put("nonceStr",nonceStr);
        params.put("secret",secret);
        params.put("status",status);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder("?");
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
        logger.info("KKone签名原串："+requestParamsEncode);
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        logger.info("KKone签名结果："+md5Value);
        return md5Value;
    }
}

package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SJ2sj 接口
 * 与第三方对接的具体实现
 */
public class SJTwoSJInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String userUnqueNo= NumberUtil.generateCharacterString(6);
        String secret=config.getSecret();

        Long timestamp=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("amount",  payOrder.getAmount().toPlainString());//金额
        params.put("pay_code", payChannel.getPayInterfaceType());// 支付方式
        params.put("nonce_string", userUnqueNo);// 用户唯一编号
        params.put("return_url", payOrder.getReturnUrl());// 支付结果URL
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("app_id", appId);// 商户ID
        params.put("store_sn", payOrder.getOrderNo());
        params.put("timestamp", timestamp);

        params.put("sign", generateCreateSign( payOrder.getAmount().toPlainString(),payOrder.getOrderNo(),appId,timestamp.toString()
                ,userUnqueNo, payChannel.getPayInterfaceType(), secret));// 签名
       String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("SJ2SJ:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObjects = JSON.parseObject(result);
            if(jsonObjects.getIntValue("code")==0){
                JSONObject jsonObject = JSON.parseObject(jsonObjects.getString("data"));
                return new ApiResult().ok(jsonObjects.getString("msg")).inject(jsonObject.getString("url"));
            }
            return new ApiResult().fail(jsonObjects.getString("msg"));
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到SJ2SJ支付结果的异步通知...");
        logger.info("咸鱼异步通知请求参数："+request);
        String amount=request.getParameter("amount");
        String order_sn=request.getParameter("order_sn");
        String out_order_sn=request.getParameter("out_order_sn");
        String timestamp=request.getParameter("timestamp");
        String nonce_string=request.getParameter("nonce_string");
        String pay_code=request.getParameter("pay_code");
        String status=request.getParameter("status");
        String signature=request.getParameter("signature");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(out_order_sn);
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
            String sign=notifySign(amount,out_order_sn,appId,timestamp,nonce_string,pay_code,serect );
            if (signature.equals(sign)) {
                if(status.equals("SUCCESS")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return "success";
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
      /*  HashMap hashMap = JSON.parseObject(jsonData, HashMap.class);
        model.addAttribute("data",hashMap);
        model.addAttribute("createOrderUrl",hashMap.get("createOrderUrl"));
        return "jump";*/
      return "order";
    }


    /**
     * 创建 签名
     * @description 生成参数签名
     * @Param storeSn 商户订单号
     * @Param amount 金额
     * @Param payCode 支付通道
     * @Param appId 商户的APPID
     * @Param timestamp 时间戳
     * @Param nonceString  随机数
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String generateCreateSign( String amount,String storeSn, String appId,
                                              String timestamp, String nonceString ,String payCode,String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("amount",amount);
        params.put("store_sn",storeSn);
        params.put("app_id",appId);
        params.put("timestamp",timestamp);
        params.put("nonce_string",nonceString);
        params.put("pay_code",payCode);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
            requestUrl.append(key).append("=");
            requestUrl.append(params.get(key));
            requestUrl.append("&");
        }
        String requestParamsEncode= requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        String lastValue=md5Value.substring(8,24);
        String md5LastValue = EncryptUtil.encodeMD5(lastValue+secret).toUpperCase();
        return md5LastValue;
    }

    /**
     * 回调 签名
     * @description 生成参数签名
     * @Param storeSn 商户订单号
     * @Param amount 金额
     * @Param payCode 支付通道
     * @Param appId 商户的APPID
     * @Param timestamp 时间戳
     * @Param nonceString  随机数
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
                                      private static String notifySign( String amount,String outOrderSn, String appId,
                                              String timestamp, String nonceString ,String payCode,String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("amount",amount);
        params.put("out_order_sn",outOrderSn);
        params.put("app_id",appId);
        params.put("timestamp",timestamp);
        params.put("nonce_string",nonceString);
        params.put("pay_code",payCode);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
            requestUrl.append(key).append("=");
            requestUrl.append(params.get(key));
            requestUrl.append("&");
        }
        String requestParamsEncode= requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
        String md5Value = EncryptUtil.encodeMD5(requestParamsEncode).toLowerCase();
        String lastValue=md5Value.substring(8,24);
        String md5LastValue = EncryptUtil.encodeMD5(lastValue+secret).toUpperCase();
        return md5LastValue;
    }


}

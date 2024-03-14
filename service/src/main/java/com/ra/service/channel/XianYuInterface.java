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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GodsPay 接口
 * 与第三方对接的具体实现
 */
public class XianYuInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String userUnqueNo= NumberUtil.generateCharacterString(6);
        String secret=config.getSecret();

        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long timestamp=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Amount",  payOrder.getAmount().multiply(BigDecimal.valueOf(100)));//金额 单位分
        params.put("PayChannel", payChannel.getPayInterfaceType());// 支付方式
        params.put("OutUserId", userUnqueNo);// 用户唯一编号
        params.put("NotifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("AppID", appId);// 商户ID
        params.put("OrderCode", payOrder.getOrderNo());
        params.put("CreateTime", sdf.format(timestamp));

        params.put("Sign", generateCreateSign(payOrder.getOrderNo(),payOrder.getAmount().multiply(BigDecimal.valueOf(100)),payChannel.getPayInterfaceType(),params.get("NotifyUrl").toString()
                ,appId, params.get("CreateTime").toString(),userUnqueNo, secret));// 签名
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), JSON.toJSONString(params));
        logger.info("XianYuInterface:咸鱼创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObjects = JSON.parseObject(result);
            JSONObject jsonObject = JSON.parseObject(jsonObjects.getString("Result"));
            if(jsonObject.getBoolean("Result")){
                return new ApiResult().ok(jsonObject.getString("ErrMsg")).inject(jsonObject.getString("H5Url"));
            }
            return new ApiResult().fail(jsonObject.getString("ErrMsg"));
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到咸鱼支付结果的异步通知...");
        String body = getBody(request);
        if(org.springframework.util.StringUtils.isEmpty(body)){
            return "回调数据为："+body;
        }
        logger.info("咸鱼异步通知请求参数："+body);
        JSONObject jsonObject = JSON.parseObject(body);
        String appKey=jsonObject.getString("AppKey");
        String orderCode=jsonObject.getString("OrderCode");
        String sign=jsonObject.getString("Sign");
        String codeNo=jsonObject.getString("CodeNo");
        String orderState=jsonObject.getString("OrderState");
        String msg=jsonObject.getString("Msg");
        String amount=jsonObject.getString("Amount");
        String fee=jsonObject.getString("Fee");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderCode);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)/100))!=0){
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

        String serect=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String signature=generateBackSign(orderCode,amount,msg,fee,appKey,orderState,codeNo,serect );
            if (signature.equals(sign)) {
                if(orderState.equals("2")){
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
        return "null";
    }


    /**
     * 创建订单签名
     * @description 生成参数签名
     * @Param orderCode 商户订单号
     * @Param amount 金额
     * @Param payChannel 支付通道
     * @Param notifyUrl 回调地址
     * @Param appId 商户的APPID
     * @Param createTime 创建时间
     * @Param outUserId  用户唯一编号
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String generateCreateSign(String orderCode, BigDecimal amount,
                                            String payChannel, String notifyUrl,
                                            String appId, String createTime, String outUserId, String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("OrderCode",orderCode);
        params.put("Amount",amount.toString());
        params.put("PayChannel",payChannel);
        params.put("NotifyUrl",notifyUrl);
        params.put("AppID",appId);
        params.put("CreateTime",createTime);
        params.put("OutUserId",outUserId);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
                //requestUrl.append(URLEncoder.encode(params.get(key), "UTF-8"));
                requestUrl.append(params.get(key));//UnsupportedEncodingException
        }
        logger.info("咸鱼创建订单签名原串："+secret + requestUrl.toString() + secret);
        String md5Value = EncryptUtil.encodeMD5(secret + requestUrl.toString() + secret).toLowerCase();
        return md5Value;
    }

    /**
     * 回调订单签名
     * @description 生成参数签名
     * @Param orderCode 商户订单号
     * @Param amount 金额
     * @Param payChannel 支付通道
     * @Param notifyUrl 回调地址
     * @Param appId 商户的APPID
     * @Param createTime 创建时间
     * @Param outUserId  用户唯一编号
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String generateBackSign(String orderCode, String amount,
                                             String msg, String fee,
                                             String appKey, String orderState, String codeNo, String secret) {

        Map<String,String> params=new HashMap<>();
        params.put("OrderCode",orderCode);
        params.put("Amount",amount);
        params.put("Fee",fee);
        params.put("Msg",msg);
        params.put("AppKey",appKey);
        params.put("OrderState",orderState);
        params.put("CodeNo",codeNo);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
                if(!org.springframework.util.StringUtils.isEmpty(params.get(key))){
                    requestUrl.append(params.get(key));//UnsupportedEncodingException
                }
        }
        logger.info("异步回调签名原串："+secret + requestUrl.toString() + secret);
        String md5Value = EncryptUtil.encodeMD5(secret + requestUrl.toString() + secret).toLowerCase();
        return md5Value;
    }
}

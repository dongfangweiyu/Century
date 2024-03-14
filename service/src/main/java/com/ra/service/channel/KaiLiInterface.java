package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.service.channel.CommonPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * http://mch.kailipay.com/x_mch/src/views/dev/pay_doc/pay.html 接口
 * 与凯利第三方对接的具体实现
 */
public class KaiLiInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道缺少接口配置...");

        String mchId=config.getAppId();
        String secret=config.getSecret();
        String appId="";
        try{
            JSONObject configJsonObject = JSON.parseObject(config.getJson());
            appId=configJsonObject.getString("appId");
//            Assert.hasText(appId,"自定义JSON参数配置错误");
        }catch (Exception e){
//            return new ApiResult().fail("第三方通道自定义JSON参数配置错误");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("mchId", mchId);//
        params.put("appId", appId);//
        params.put("productId",  payChannel.getPayInterfaceType());//支付产品
        params.put("mchOrderNo", payOrder.getOrderNo());// 商户订单号
        params.put("currency", "cny");
        params.put("amount", payOrder.getAmount().multiply(BigDecimal.valueOf(100)));
//        params.put("clientIp","210.73.10.148");
//        params.put("device","ios10.3.1");
        params.put("returnUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("subject","购买商品");
        params.put("body",payOrder.getOrderDesc());
        params.put("param1","{}");
        params.put("param2","{}");
        params.put("extra","{}");
        params.put("sign",getSign(params,secret));

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("KaiLiInterface:创建订单返回JSON："+result);

        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("retCode").equalsIgnoreCase("SUCCESS")){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("url"));
            }else{
                String  data=jsonObject.getString("retMsg");
                return new ApiResult().fail(data);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");

        try {
            request.setCharacterEncoding("utf-8");
        } catch (UnsupportedEncodingException e) {
            return "FAIL";
        }

        Map<String, Object> paramMap = getRequestParams(request);

        String mchOrderNo=(String) paramMap.get("mchOrderNo");
        String sign=(String)paramMap.get("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(mchOrderNo);
        if(payOrder==null){
            return "订单不存在";
        }
//        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Integer.parseInt(money)))!=0){
//            return "金额不对";
//        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "payChannel is null";
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
        String secret=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            paramMap.remove("sign");
            String md5sign=getSign(paramMap,secret);
            if (sign.equals(md5sign)) {
                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "success";
                }
                return "订单状态不为待支付";
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
}
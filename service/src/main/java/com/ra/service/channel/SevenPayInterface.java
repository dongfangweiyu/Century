package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * u.sevenpay.monster 接口
 * 与7付支付对接的具体实现
 */
public class SevenPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        String userUnqueNo= NumberUtil.generateCharacterString(6);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("appid", appId);//
        params.put("out_trade_no", payOrder.getOrderNo());//
        params.put("pay_type", payChannel.getPayInterfaceType());// 支付方式
        params.put("callback_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("success_url", payOrder.getReturnUrl());// 支付成功的URL
        params.put("amount", payOrder.getAmount().setScale(2).toPlainString());
        params.put("error_url",payOrder.getReturnUrl());//支付失败的地址
        params.put("return_type","app");
        params.put("out_uid", userUnqueNo);
        params.put("version", "v1.1");
        params.put("sign", getSign(params,secret).toUpperCase());//
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        //{"status":1,"msg":"成功订单","sdorderno":"商户订单号","total_fee":"订单金额","sdpayno":"平台订单号"}
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        /* String customer_id=request.getParameter("customer_id");*/
        String out_trade_no=request.getParameter("out_trade_no");

        String sign=request.getParameter("sign");
        String amount=request.getParameter("amount");
        String callbacks=request.getParameter("callbacks");
        Map<String, Object> requestParams = getRequestParams(request);
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(out_trade_no);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)))!=0){
            return "金额不对";
        }
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

       // String appId=config.getAppId();
        String secret=config.getSecret();
        logger.info("密钥="+secret);
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");
            String md5sign=getSign(requestParams,secret).toUpperCase();
            if (md5sign.equals(sign)) {
                if(callbacks.equals("CODE_SUCCESS")){
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
        HashMap hashMap = JSON.parseObject(jsonData, HashMap.class);
        model.addAttribute("data",hashMap);
        model.addAttribute("createOrderUrl",hashMap.get("createOrderUrl"));
        return "jump";
    }
}



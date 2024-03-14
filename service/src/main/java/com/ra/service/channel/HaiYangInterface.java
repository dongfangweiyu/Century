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
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

/**
 * https://sh6698.com/Agent/接口
 * 与海洋支付第三方对接的具体实现
 */
public class HaiYangInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pay_memberid", appId);//
        params.put("price",payOrder.getAmount().setScale(2).toPlainString());
        params.put("class", Integer.parseInt(payChannel.getPayInterfaceType()));// 支付方式
        params.put("pay_notifyurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("pay_callbackurl", payOrder.getReturnUrl());// 支付结果URL
        params.put("pay_orderid", payOrder.getOrderNo());//
        params.put("addtime",String.valueOf(System.currentTimeMillis()).substring(0,10));//
        params.put("md5sign",getSign(params,token).toUpperCase());
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(),params);
        logger.info("HaiYangInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("status").equals("success")){
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonData.getString("pay_url"));
            }else{
                String  msg=jsonObject.getString("msg");
                return new ApiResult().fail(msg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String memberid=request.getParameter("memberid");
        String orderid=request.getParameter("orderid");
        String transaction_id=request.getParameter("transaction_id");
        String amount=request.getParameter("amount");
        String datetime=request.getParameter("datetime");
        String returncode=request.getParameter("returncode");
        String sign=request.getParameter("sign");

        Map<String, Object> requestParams = getRequestParams(request);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderid);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)))!=0){
            return "金额不对";
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "FAIL";
        }

        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        if(config==null){
            return "FAIL";
        }
        boolean b= verifyIP(config,request);
        if(!b){
            return "回调IP不正确";
        }
        String token=config.getSecret();
        try{
            requestParams.remove("sign");
            String md5sign=getSign(requestParams,token).toUpperCase();
            if (sign.equals(md5sign)) {
                if(returncode.equals("00")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return "OK";
                    }
                    return "订单已完成,请结束回调";
                }
                return "支付失败";
            }
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



package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
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
import java.util.HashMap;
import java.util.Map;

/**
 * hengsenmao.com/customer.php 接口
 * 与北斗星第三方对接的具体实现
 */
public class BeiDouXingInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("customer_id", appId);//
        params.put("money",payOrder.getAmount().toPlainString());
        params.put("pay_type", payChannel.getPayInterfaceType());// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("return_url", payOrder.getReturnUrl());// 支付结果URL
        params.put("out_order_id", payOrder.getOrderNo());//
        params.put("sign",getSign(params,token).toUpperCase());
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
       /* String customer_id=request.getParameter("customer_id");*/
        String out_order_id=request.getParameter("out_order_id");
       /* String money=request.getParameter("money");
        String order_id=request.getParameter("order_id");
        String pay_time=request.getParameter("pay_time");*/
        String notice=request.getParameter("notice");
        String notifySign=request.getParameter("sign");
        Map<String, Object> requestParams = getRequestParams(request);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(out_order_id);
        if(payOrder==null){
            return "订单不存在";
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

        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");
            String sign=getSign(requestParams,token).toUpperCase();
            if (sign.equals(notifySign)) {
                if(notice.equals("1")) {
                    //如果验签成功
                    if (payOrder.getStatus() == OrderStatusEnum.PROCESS) {

                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder, null);//更新为成功订单
                        return "ok";
                    }
                }else{
                    return "支付失败";
                }
            }
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



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
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * https://merchant.rayfpay.cn/user/index.do 接口
 * 与Rayf支付第三方对接的具体实现
 */
public class RayfPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();
        String userUnqueNo= NumberUtil.generateCharacterString(6);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_no", appId);//
        params.put("out_trade_no", payOrder.getOrderNo());//
        params.put("type", payChannel.getPayInterfaceType());// 支付方式
        logger.info("RayfPayInterface:异步回调地址："+IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");
        logger.info("RayfPayInterface:同步回调地址："+payOrder.getReturnUrl());
        /*params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("pay_user_id", payOrder.getReturnUrl());// 支付结果URL*/
        params.put("total_fee", payOrder.getAmount());
        params.put("back_params","");
        params.put("pay_user_id",userUnqueNo);
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String status=request.getParameter("status");
        String total_fee=request.getParameter("total_fee");
        String out_trade_no=request.getParameter("out_trade_no");
        String back_params=request.getParameter("back_params");
        String type=request.getParameter("type");
        String sign=request.getParameter("sign");
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(out_trade_no);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(total_fee)))!=0){
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

        String appId=config.getAppId();
        String secret=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String md5sign=notifySign(out_trade_no,total_fee,appId,secret);
            if (sign.equals(md5sign)) {
                if(status.equals("1")){
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
        HashMap hashMap = JSON.parseObject(jsonData, HashMap.class);
        model.addAttribute("data",hashMap);
        model.addAttribute("createOrderUrl",hashMap.get("createOrderUrl"));
        return "jump";
    }


    /**
     * 异步回调的签名
     * @param out_trade_no
     * @param total_fee
     * @param appId
     * @param secret
     * @return
     */
    private String notifySign(String out_trade_no,String total_fee,String appId,String secret){
        StringBuffer sb=new StringBuffer();
        sb.append(out_trade_no);
        sb.append(total_fee);
        sb.append(appId);
        sb.append(secret);
        logger.info("签名源串="+sb.toString());
        String s = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return s;

    }
}



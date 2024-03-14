package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  接口
 * 与美盈支付第三方对接的具体实现
 */
public class MeiYinPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("mid", appId);//
        params.put("oid", payOrder.getOrderNo());//
        params.put("amt", payOrder.getAmount());
        params.put("way", payChannel.getPayInterfaceType());// 支付方式
        params.put("back", payOrder.getReturnUrl());// 支付结果URL
        params.put("notify", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL

        List<Object> signList=new ArrayList<>();
        signList.add(appId);
        signList.add(payOrder.getOrderNo());
        signList.add(payOrder.getAmount());
        signList.add(payChannel.getPayInterfaceType());
        signList.add(payOrder.getReturnUrl());
        signList.add(IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");
        params.put("sign", generalSign(signList,secret));//
        params.put("time",  String.valueOf(System.currentTimeMillis()).substring(0,10));//10位时间戳
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String mid=request.getParameter("mid");
        String oid=request.getParameter("oid");
        String amt=request.getParameter("amt");
        String way=request.getParameter("way");
        String code=request.getParameter("code");
        String sign = request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(oid);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amt)))!=0){
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

        String appId=config.getAppId();
        String secret=config.getSecret();
        try{
            List<Object> signList=new ArrayList<>();
            signList.add(appId);
            signList.add(oid);
            signList.add(amt);
            signList.add(way);
            signList.add(code);
            // 延签
            // 验证是不是支付网关发来的异步通知
            String md5sign=generalSign(signList,secret);
            if (sign.equals(md5sign)) {
                if(code.equals("100")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return "ok";
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
     * 签名
     * @param list
     * @param key
     * @return
     */
    private String generalSign(List<Object> list, String key){
        StringBuffer sb=new StringBuffer();
        for (Object value : list) {
            sb.append(value);
        }
        sb.append(key);
        logger.info("MeiYinPayInterface签名源串:"+sb.toString());
        String s = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("MeiYinPayInterface签名加密结果:"+s);
        return s;
    }
}



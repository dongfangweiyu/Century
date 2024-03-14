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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * http://vip.keithkubena.com/User/Main   接口
 * 与新腾龙支付第三方对接的具体实现
 */
public class TengLongPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道缺少接口配置...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        String banktype=payChannel.getPayInterfaceType();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("partner", appId);//
        params.put("paymoney", payOrder.getAmount());
        params.put("ordernumber",payOrder.getOrderNo());
        params.put("hrefbackurl", payOrder.getReturnUrl());// 支付结果URL
        params.put("callbackurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("banktype", banktype);//支付产品
        params.put("attach",payOrder.getAttach());
        params.put("sign",generalCreateSign(appId,banktype,payOrder.getAmount(),payOrder.getOrderNo(),params.get("callbackurl").toString(),secret));

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("TengLongPayInterface:创建订单返回JSON："+result);

        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getBooleanValue("success")){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("url"));
            }else{
                String  data=jsonObject.getString("responsedesc");
                return new ApiResult().fail(data);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");
        Map<String, Object> paramMap = getRequestParams(request);

        String ordernumber=paramMap.get("ordernumber").toString();
        String orderstatus=paramMap.get("orderstatus").toString();
        String paymoney=paramMap.get("paymoney").toString();
        String sign=paramMap.get("sign").toString();

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(ordernumber);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(paymoney)))!=0){
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
            String md5sign=generalNotifySign(appId,ordernumber,orderstatus,paymoney,secret);
            if (sign.equals(md5sign)) {
                //如果验签成功
                if(orderstatus.equalsIgnoreCase("1")){
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
        return "order";
    }


    /**
     * 生成签名
     * @return
     */
    private String generalCreateSign(String partner,String banktype,BigDecimal paymoney,
                               String ordernumber,String callbackurl,String key ){
        StringBuffer sb=new StringBuffer();
        sb.append("partner="+partner);
        sb.append("&banktype="+banktype);
        sb.append("&paymoney="+paymoney);
        sb.append("&ordernumber="+ordernumber);
        sb.append("&callbackurl="+callbackurl);
        sb.append(key);
        logger.info("TengLongPayInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("TengLongPayInterface签名加密结果："+md5Value);
        return md5Value;
    }


    /**
     * 生成签名
     * @return
     */
    private String generalNotifySign(String partner, String ordernumber,
                                    String orderstatus,String paymoney,String key ){
        StringBuffer sb=new StringBuffer();
        sb.append("partner="+partner);
        sb.append("&ordernumber="+ordernumber);
        sb.append("&orderstatus="+orderstatus);
        sb.append("&paymoney="+paymoney);
        sb.append(key);
        logger.info("TengLongPayInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("TengLongPayInterface签名加密结果："+md5Value);
        return md5Value;
    }
}
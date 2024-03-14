package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * http://59.153.74.82:10088 接口
 * 与UC支付宝第三方对接的具体实现
 */
public class UCPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("merchantNum", appId);//
        params.put("orderNo", payOrder.getOrderNo());//
        params.put("payType", payChannel.getPayInterfaceType());// 支付方式
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("returnUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("amount", payOrder.getAmount());
        params.put("attch","1");
        params.put("ip","220.181.38.148");
        params.put("province", "");
        params.put("city", "");
        params.put("cityCode", "");
        params.put("sign", generalSign(appId,payOrder.getOrderNo(),params.get("amount").toString(),params.get("notifyUrl").toString(),secret));//
        //{"status":1,"msg":"成功订单","sdorderno":"商户订单号","total_fee":"订单金额","sdpayno":"平台订单号"}
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("UCPayInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==200){
                JSONObject jsonData = jsonObject.getJSONObject("data");
                return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonData.getString("payUrl"));
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
        String merchantNum=request.getParameter("merchantNum");
        String orderNo=request.getParameter("orderNo");
        String platformOrderNo=request.getParameter("platformOrderNo");
        String amount=request.getParameter("amount");
        String attch=request.getParameter("attch");
        String state=request.getParameter("state");
        String payTime=request.getParameter("payTime");
        String actualPayAmount=request.getParameter("actualPayAmount");
        String sign=request.getParameter("sign");
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
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
            String md5sign=notifySign(state,merchantNum,orderNo,amount,secret);
            if (sign.equals(md5sign)) {
                if(state.equals("1")){
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
     /*   HashMap hashMap = JSON.parseObject(jsonData, HashMap.class);
        model.addAttribute("data",hashMap);
        model.addAttribute("createOrderUrl",hashMap.get("createOrderUrl"));*/
        return "null";
    }


    /**
     * 生成签名
     * @return
     */
    private String generalSign(String merchantNum,String orderNo,String amount,String notifyUrl,String key){
        StringBuffer sb=new StringBuffer();
        sb.append(merchantNum);
        sb.append(orderNo);
        sb.append(amount);
        sb.append(notifyUrl);
        sb.append(key);
        logger.info("签名源串="+sb.toString());
        String s = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return s;
    }

    /**
     * 异步通知签名方法
     * @param state
     * @param merchantNum
     * @param orderNo
     * @param amount
     * @param secret
     * @return
     */
    private String notifySign(String state,String merchantNum,String orderNo,String amount,String secret){
        StringBuffer sb=new StringBuffer();
        sb.append(state);
        sb.append(merchantNum);
        sb.append(orderNo);
        sb.append(amount);
        sb.append(secret);
        logger.info("签名源串="+sb.toString());
        String s = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return s;

    }
}



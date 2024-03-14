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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * http://www.xunhangjianzhifu.com/paydoc 接口
 * 与巡航舰第三方对接的具体实现
 */
public class XunHangJianInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Long timestamp=System.currentTimeMillis();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("uid", appId);//
        params.put("user_order_no", payOrder.getOrderNo());//
        params.put("paytype", payChannel.getPayInterfaceType());// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("return_url", payOrder.getReturnUrl());// 支付结果URL
        params.put("price", payOrder.getAmount().setScale(2).toPlainString());
        params.put("note","1");
        params.put("cuid",payOrder.getOrderNo());
        params.put("tm", sdf.format(timestamp));
        params.put("sign", generalSign(appId,params.get("price").toString(),payChannel.getPayInterfaceType(),params.get("notify_url").toString(),params.get("return_url").toString(),payOrder.getOrderNo(),secret));//
        //{"status":1,"msg":"成功订单","sdorderno":"商户订单号","total_fee":"订单金额","sdpayno":"平台订单号"}
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String body = getBody(request);
        if(StringUtils.isEmpty(body)){
            return "回调数据为："+body;
        }
        JSONObject jsonObject = JSON.parseObject(body);
        String user_order_no=jsonObject.getString("user_order_no");
        String orderno=jsonObject.getString("orderno");
        String tradeno=jsonObject.getString("tradeno");
        String price=jsonObject.getString("price");
        String realprice=jsonObject.getString("realprice");
        String status=jsonObject.getString("status");
        String sign=jsonObject.getString("sign");
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(user_order_no);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(price)))!=0){
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
            //user_order_no + orderno + tradeno + price + realprice + token）
            String md5sign=notifySign(user_order_no,orderno,tradeno,price,realprice,secret);
            if (sign.equals(md5sign)) {
                if(status.equals("3")){
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


    /**
     * 生成签名
     * @return
     */
    private String generalSign(String uid,String price,String paytype,String notify_url,String return_url,String user_order_no,String token){
        String str="uid={uid}&price={price}&paytype={paytype}&notify_url={notify_url}&return_url={return_url}&user_order_no={user_order_no}&token={token}";
        str=str.replace("{uid}",uid);
        str=str.replace("{price}",price);
        str=str.replace("{paytype}",paytype);
        str=str.replace("{notify_url}",notify_url);
        str=str.replace("{return_url}",return_url);
        str=str.replace("{user_order_no}",user_order_no);
        str=str.replace("{token}",token);
        logger.info("签名源串="+str);
        String s = EncryptUtil.encodeMD5(str).toLowerCase();
        return s;
    }

    /**
     * 异步通知签名方法
     * @param user_order_no
     * @param orderno
     * @param price
     * @param realprice
     * @param token
     * @return
     */
    private String notifySign(String user_order_no,String orderno,String tradeno,String price,String realprice,String token){
       //user_order_no + orderno + tradeno + price + realprice + token）
      //  String str="user_order_no={user_order_no}&orderno={orderno}&tradeno={tradeno}&price={price}&realprice={realprice}&{token}";
        StringBuffer sb=new StringBuffer();
        sb.append(user_order_no);
        sb.append(orderno);
        sb.append(tradeno);
        sb.append(price);
        sb.append(realprice);
        sb.append(token);
        /*str=str.replace("{user_order_no}",user_order_no);
        str=str.replace("{orderno}",orderno);
        str=str.replace("{tradeno}",tradeno);
        str=str.replace("{price}",price);
        str=str.replace("{realprice}",realprice);
        str=str.replace("{token}",token);*/
        logger.info("签名源串="+sb.toString());
        String s = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return s;

    }
}



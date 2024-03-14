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
 * http://api.hgpay1.com/UserLogin.html  皇冠支付 接口
 * 与第三方对接的具体实现
 */
public class HGPAYInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws Exception {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("partner", appId);//
        params.put("banktype", payChannel.getPayInterfaceType());
        params.put("paymoney", payOrder.getAmount());
        params.put("ordernumber", payOrder.getOrderNo());
        params.put("callbackurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 支付结果URL
        params.put("hrefbackurl", payOrder.getReturnUrl());
        params.put("attach","");
        params.put("sign",getCreateSign(appId,
                params.get("banktype").toString(),
                params.get("paymoney").toString(),
                params.get("ordernumber").toString(),
                params.get("callbackurl").toString(),
                secret));

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("HGPAYInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getBooleanValue("success")){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("url"));

//                redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),result,60*10);
//                return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
            }else{
                String  rspMsg=jsonObject.getString("responsedesc");
                return new ApiResult().fail(rspMsg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String partner=request.getParameter("partner");
        String ordernumber=request.getParameter("ordernumber");
        String orderstatus=request.getParameter("orderstatus");
        String paymoney=request.getParameter("paymoney");
        String sysnumber=request.getParameter("sysnumber");
        String attach=request.getParameter("attach");
        String sign=request.getParameter("sign");

        Map<String,Object> resp=new HashMap<>();
        resp.put("success",false);
        resp.put("responsedesc","回调验签失败");
        resp.put("ordernumber",ordernumber);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(ordernumber);
        if(payOrder==null){
            return JSON.toJSONString(resp);
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Integer.parseInt(paymoney)))!=0){
            return JSON.toJSONString(resp);
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return JSON.toJSONString(resp);
        }

        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        if(config==null){
            return JSON.toJSONString(resp);
        }
        boolean b= verifyIP(config,request);
        if(!b){
            return "回调IP不正确";
        }

        String appId=config.getAppId();
        String secret=config.getSecret();
        resp.put("partner",appId);
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String signgute=getNotifySign(appId,ordernumber,orderstatus,paymoney,secret);
            if (signgute.equalsIgnoreCase(sign)) {

                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){
                    resp.put("success",true);
                    resp.put("sign",getCallBackSign(resp.get("success").toString(),appId,resp.get("ordernumber").toString(),secret));
                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return JSON.toJSONString(resp);
                }
            }
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            e.printStackTrace();
        }
        return JSON.toJSONString(resp);
    }

    @Override
    public String parseData(Model model,PayOrder payOrder, String jsonData) throws Exception{

//        JSONObject jsonObject = JSON.parseObject(jsonData);
//        if(jsonObject.getString("Code").equalsIgnoreCase("2000")){
//
//            String qrcode=jsonObject.getString("Msg");
//            BigDecimal amount=jsonObject.getBigDecimal("totalAmount");
//            int payType=1;
//
//
//            model.addAttribute("qrcode",qrcode);//http://mall.txiat.cn/payAPI/doPay.jsp?idchannel=2032&id=MTcx
//            model.addAttribute("endTime", payOrder.getCreateTime().getTime()+1000*60*10);
//            model.addAttribute("serverTime",System.currentTimeMillis());
//            model.addAttribute("orderNo",payOrder.getOrderNo());
//            model.addAttribute("amount",amount.toPlainString());
//            model.addAttribute("payType",payType==1?"微信":"支付宝");
//            model.addAttribute("payTypeIcon",payType==1?"/static/images/wechat.png":"/static/images/alipay.png");
////            model.addAttribute("jumpBtn","weixin://dl/businessWebview/link/?appid=wx02920f7eaac6b2ef&url="+qrcode);//跳转按钮
////            model.addAttribute("jumpBtn",qrcode);//跳转按钮
////            model.addAttribute("jumpBtn","https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx15182851153781676c43176e1569397800&package=1037687096");//跳转按钮
//        }else{
//            throw new IllegalArgumentException(jsonObject.getString("Msg"));
//        }
        return "order";
    }


    /**
     * 创建订单签名
     * @description 生成参数签名
     * @Param outOrderNo 商户订单号
     * @Param amount 金额
     * @Param payType 支付通道
     * @Param attach 订单附加参数
     * @Param appId 商户的APPID
     * @Param timestamp 13位时间戳
     * @Param nonceStr 业务流水随机字符串
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String getCreateSign(String partner , String banktype ,
                                   String paymoney ,String ordernumber,String callbackurl,String key) {
        StringBuffer sb=new StringBuffer();
        sb.append("partner="+partner);
        sb.append("&banktype="+banktype);
        sb.append("&paymoney="+paymoney);
        sb.append("&ordernumber="+ordernumber);
        sb.append("&callbackurl="+callbackurl);
        sb.append(key);
        logger.info("HGPAYInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("HGPAYInterface签名加密结果："+md5Value);
        return md5Value;
    }


    /**
     * 回调订单签名
     * @description 生成参数签名
     * @Param outOrderNo 商户订单号
     * @Param amount 金额
     * @Param payType 支付通道
     * @Param attach 订单附加参数
     * @Param appId 商户的APPID
     * @Param timestamp 13位时间戳
     * @Param nonceStr 业务流水随机字符串
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String getNotifySign(String partner , String ordernumber , String orderstatus ,
                                        String paymoney ,String secret) {
        StringBuffer sb=new StringBuffer();
        sb.append("partner="+partner);
        sb.append("&ordernumber="+ordernumber);
        sb.append("&orderstatus="+orderstatus);
        sb.append("&paymoney="+paymoney);
        sb.append(secret);
        logger.info("HGPAYInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("HGPAYInterface签名加密结果："+md5Value);
        return md5Value;
    }

    /**
     * 收到回调返回签名
     * @Param appId 商户的APPID
     * @Param timestamp 13位时间戳
     * @Param nonceStr 业务流水随机字符串
     * @Param secret 商户的秘钥
     * @return 回调参数签名
     */
    private static String getCallBackSign(String success , String partner , String ordernumber ,
                                        String secret) {
        StringBuffer sb=new StringBuffer();
        sb.append("success="+success);
        sb.append("&partner="+partner);
        sb.append("&ordernumber="+ordernumber);
        sb.append(secret);
        logger.info("HGPAYInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("HGPAYInterface签名加密结果："+md5Value);
        return md5Value;
    }

}



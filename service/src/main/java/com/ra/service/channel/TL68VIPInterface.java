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
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * http://pay.tl68vip.com/thirdPay 微信扫码 接口
 * 与第三方对接的具体实现
 */
public class TL68VIPInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws Exception {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("merchantId", appId);//
        params.put("totalAmount", payOrder.getAmount());
        params.put("tradeNo", payOrder.getOrderNo());
        params.put("model", payChannel.getPayInterfaceType());// 支付方式
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 支付结果URL
        params.put("returnUrl", payOrder.getReturnUrl());//订单金额
        params.put("remark",payOrder.getOrderDesc());//充值面额，按支付宝固定面额
        params.put("sign",getCreateSign(appId,
                params.get("model").toString(),params.get("notifyUrl").toString(),
                params.get("remark").toString(),
                payOrder.getReturnUrl(),
                params.get("totalAmount").toString(),
                params.get("tradeNo").toString(),
                secret));

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("TL68VIPInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("Code").equalsIgnoreCase("2000")){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("Msg"));

//                redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),result,60*10);
//                return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
            }else{
                String  rspMsg=jsonObject.getString("Msg");
                return new ApiResult().fail(rspMsg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String downNo=request.getParameter("downNo");
        String tradeNo=request.getParameter("tradeNo");
        String realAmount=request.getParameter("realAmount");
        String Result=request.getParameter("Result");
        String merchantId=request.getParameter("merchantId");
        String Code=request.getParameter("Code");
        String Msg=request.getParameter("Msg");
        String notifyTime=request.getParameter("notifyTime");
        String remark=request.getParameter("remark");
        String sign=request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(downNo);
        if(payOrder==null){
            return "订单不存在";
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
            // 延签
            // 验证是不是支付网关发来的异步通知
            String signgute=getNotifySign(Code,downNo,appId,Msg,notifyTime,realAmount,remark,Result,tradeNo,secret);
//            String signgute=getCreateSign(orderId,appId,"0.0.1",orderAmt,"1000",secret);
            if (signgute.equalsIgnoreCase(sign)) {

                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "suc";
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
    private static String getCreateSign(String merchantId , String model , String notifyUrl ,
                                   String remark ,String returnUrl,String totalAmount,String tradeNo,String secret) {
        StringBuffer sb=new StringBuffer();
        sb.append("merchantId="+merchantId);
        sb.append("model="+model);
        sb.append("notifyUrl="+notifyUrl);
        sb.append("remark="+remark);
        sb.append("returnUrl="+returnUrl);
        sb.append("totalAmount="+totalAmount);
        sb.append("tradeNo="+tradeNo);
        sb.append(secret);
        logger.info("TL68VIPInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("TL68VIPInterface签名加密结果："+md5Value);
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
    private static String getNotifySign(String Code , String downNo , String merchantId ,
                                        String Msg ,String notifyTime,String realAmount,String remark,
                                        String Result,String tradeNo,String secret) {

        StringBuffer sb=new StringBuffer();
        sb.append("Code="+Code);
        sb.append("Msg="+Msg);
        sb.append("Result="+Result);
        sb.append("downNo="+downNo);
        sb.append("merchantId="+merchantId);
        sb.append("notifyTime="+notifyTime);
        sb.append("realAmount="+realAmount);
        sb.append("remark="+remark);
        sb.append("tradeNo="+tradeNo);
        sb.append(secret);
        logger.info("TL68VIPInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        logger.info("TL68VIPInterface签名加密结果："+md5Value);
        return md5Value;
    }

}



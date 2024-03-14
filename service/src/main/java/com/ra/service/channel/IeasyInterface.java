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
import java.util.*;

/**
 * http://merch.ieasyform.com:6328/x_mch/start/index.html#/user/login 接口
 * 与第三方对接的具体实现
 */
public class IeasyInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws Exception {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("mchId", appId);                               // 商户ID
//        params.put("appId", appId);                             // 应用ID,非必填
        params.put("mchOrderNo", payOrder.getOrderNo());     // 商户订单号
        params.put("productId", payChannel.getPayInterfaceType());                       // 支付产品
        params.put("amount", payOrder.getAmount().multiply(BigDecimal.valueOf(100)));                                // 支付金额,单位分
        params.put("currency", "cny");                            // 币种, cny-人民币
        params.put("clientIp", "");                 // 用户地址,微信H5支付时要真实的
        params.put("device", "");                              // 设备
        params.put("subject", payOrder.getOrderDesc());
        params.put("body", payOrder.getOrderDesc());
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");                       // 回调URL
        params.put("returnUrl",payOrder.getReturnUrl());
        params.put("param1", "");                                 // 扩展参数1
        params.put("param2", "");                                 // 扩展参数2
        //paramMap.put("extra", "{\"productId\":\"100\"}");  // 附加参数
        params.put("extra", "");

        //{"h5_info": {"type":"Wap","wap_url": "https://pay.qq.com","wap_name": "腾讯充值"}}

        String reqSign = getSign(params, secret);
        params.put("sign", reqSign);                              // 签名

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("TL68VIPInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("retCode").equalsIgnoreCase("SUCCESS")){

                JSONObject payParams = jsonObject.getJSONObject("payParams");
                String codeUrl = payParams.getString("codeUrl");
                return new ApiResult().ok("订单创建成功").inject(codeUrl);

//                redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),result,60*10);
//                return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
            }else{
                String  rspMsg=jsonObject.getString("retMsg");
                return new ApiResult().fail(rspMsg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        Map<String,Object> paramMap = getRequestParams(request);
        logger.info("支付中心通知请求参数,paramMap={}", paramMap);

        String sign = (String) paramMap.get("sign");
        String mchOrderNo=(String) paramMap.get("mchOrderNo");//商户订单号

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(mchOrderNo);
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

            //重设map
            paramMap.put("mchId",appId);
            paramMap.remove("appId");
            paramMap.remove("sign");

            // 延签
            // 验证是不是支付网关发来的异步通知
            String signgute=getSign(paramMap,secret);
            if (signgute.equalsIgnoreCase(sign)) {

                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "success";
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
}



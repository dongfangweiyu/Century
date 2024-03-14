package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
import com.ra.common.utils.*;
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
 * 话费 接口
 * 与第三方对接的具体实现
 */
public class TXIATInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws Exception {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");
        checkPrice(payOrder.getAmount());//检测传入的金额是否符合通道金额规范

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", payOrder.getOrderNo());//
        params.put("merchantId", appId);
        params.put("version", "0.0.1");// 支付方式
        params.put("bgUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("pageUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("orderAmt", payOrder.getAmount().multiply(BigDecimal.valueOf(100)));//订单金额
        params.put("price",payOrder.getAmount());//充值面额，按支付宝固定面额
        params.put("phoneNum",getPhoneNum());//虚拟号码
        params.put("bizCode", "1000");
        params.put("productName", URLEncoder.encode(payOrder.getOrderDesc(),"utf-8"));//
        params.put("sign",getCreateSign(payOrder.getOrderNo(),
                appId,params.get("version").toString(),
                params.get("orderAmt").toString(),
                params.get("bizCode").toString(),
                params.get("phoneNum").toString(),
                params.get("price").toString(),
                secret));

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("TXIATInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("rspCode").equalsIgnoreCase("00")){
                redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),result,60*10);
                return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
            }else{
                String  rspMsg=jsonObject.getString("rspMsg");
                return new ApiResult().fail(rspMsg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String orderAmt=request.getParameter("orderAmt");
        String upOrderId=request.getParameter("upOrderId");
        String status=request.getParameter("status");
        String orderId=request.getParameter("orderId");
        String merchantId=request.getParameter("merchantId");
        String sign=request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderId);
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
            String signgute=getNotifySign(appId,orderAmt,orderId,status,upOrderId,secret);
//            String signgute=getCreateSign(orderId,appId,"0.0.1",orderAmt,"1000",secret);
            if (signgute.equalsIgnoreCase(sign)) {

                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "ok";
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

        JSONObject jsonObject = JSON.parseObject(jsonData);
        if(jsonObject.getString("rspCode").equalsIgnoreCase("00")){

            String qrcode=jsonObject.getString("payUrl");
            BigDecimal amount=jsonObject.getBigDecimal("orderAmt");
            int payType=1;

            model.addAttribute("qrcode",qrcode);//http://mall.txiat.cn/payAPI/doPay.jsp?idchannel=2032&id=MTcx
            model.addAttribute("endTime", payOrder.getCreateTime().getTime()+1000*60*10);
            model.addAttribute("serverTime",System.currentTimeMillis());
            model.addAttribute("orderNo",payOrder.getOrderNo());
            model.addAttribute("amount",amount.divide(BigDecimal.valueOf(100)).toPlainString());
            model.addAttribute("payType",payType==1?"微信":"支付宝");
            model.addAttribute("payTypeIcon",payType==1?"/static/images/wechat.png":"/static/images/alipay.png");
//            model.addAttribute("jumpBtn","weixin://dl/businessWebview/link/?appid=wx02920f7eaac6b2ef&url="+qrcode);//跳转按钮
//            model.addAttribute("jumpBtn",qrcode);//跳转按钮
//            model.addAttribute("jumpBtn","https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx15182851153781676c43176e1569397800&package=1037687096");//跳转按钮
        }else{
            throw new IllegalArgumentException(jsonObject.getString("rspMsg"));
        }
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
    private static String getCreateSign(String orderId , String merchantId , String version ,
                                   String orderAmt ,String bizCode,String phoneNum,String price,String secret) {
        StringBuffer sb=new StringBuffer();
        sb.append("bizCode="+bizCode);
        sb.append("&merchantId="+merchantId);
        sb.append("&orderAmt="+orderAmt);
        sb.append("&orderId="+orderId);
        sb.append("&phoneNum="+phoneNum);
        sb.append("&price="+price);
        sb.append("&version="+version);
        sb.append("&key="+secret);
        logger.info("TXIATInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toUpperCase();
        logger.info("TXIATInterface签名加密结果："+md5Value);
        return md5Value;
    }



    /**
     * 通知订单签名
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
    private static String getNotifySign(String merchantId ,String orderAmt ,String orderId ,
                                        String status,String upOrderId,String secret) {
        StringBuffer sb=new StringBuffer();
        sb.append("merchantId="+merchantId);
        sb.append("&orderAmt="+orderAmt);
        sb.append("&orderId="+orderId);
        sb.append("&status="+status);
        sb.append("&upOrderId="+upOrderId);
        sb.append("&key="+secret);
        logger.info("TXIATInterface签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toUpperCase();
        logger.info("TXIATInterface签名加密结果："+md5Value);
        return md5Value;
    }


    /**
     * 随机取一个虚拟号码
     * @return
     */
    private static String getPhoneNum(){
        String phoneStr="1390770,1390771,1390772,1390773,1390774,1390775,1390776,1390777,1390778,1390779,1390780,1390781,1390782,1390783,1390784,1390785,1390786,1390787,1390788,1390789,1397700,1397701,1397702,1397703,1397704,1397705,1397706,1397707,1397708,1397709,1397710,1397711,1397712,1397713,1397714,1397715,1397716,1397717,1397718,1397719,1397720,1397721,1397722,1397723,1397724,1397725,1397726,1397727,1397728,1397729,1397730,1397731,1397732,1397733,1397734,1397735,1397736,1397737,1397738,1397739,1397740,1397741,1397742,1397743,1397744,1397745,1397746,1397747,1397748,1397749,1397750,1397751,1397752,1397753,1397754,1397755,1397756,1397757,1397758,1397759,1397760,1397761,1397762,1397763,1397764,1397765,1397766,1397767,1397768,1397769,1397770,1397771,1397772,1397773,1397774,1397775,1397776,1397777,1397778,1397779,1397780,1397781,1397782,1397783,1397784,1397785,1397786,1397787,1397788,1397789,1397790,1397791,1397792,1397793,1397794,1397795,1397796,1397797,1397798,1397799,1397800,1397801,1397802,1397803,1397804,1397805,1397806,1397807,1397808,1397809,1397810,1397811,1397812,1397813,1397814,1397815,1397816,1397817,1397818,1397819,1397820,1397821,1397822,1397823,1397824,1397825,1397826,1397827,1397828,1397829,1397830,1397831,1397832,1397833,1397834,1397835,1397836,1397837,1397838,1397839,1397840,1397841,1397842,1397843,1397844,1397845,1397846,1397847,1397848,1397849,1397850,1397851,1397852,1397853,1397854,1397855,1397856,1397857,1397858,1397859,1397860,1397861,1397862,1397863,1397864,1397865,1397866,1397867,1397868,1397869,1397870,1397871,1397872,1397873,1397874,1397875,1397876,1397877,1397878,1397879,1397880,1397881,1397882,1397883,1397884,1397885,1397886,1397887,1397888,1397889,1397890,1397891,1397892,1397893,1397894,1397895,1397896,1397897,1397898,1397899";
        String[] phoneArr = phoneStr.split(",");
        int index = (int) (Math.random() * phoneArr.length);
        String prefx = phoneArr[index];
        return prefx+NumberUtil.generateDigitalString(4);
    }

    /**
     * 检测充值面额
     * @return
     */
    private static void checkPrice(BigDecimal amount){
        BigDecimal[] limits=new BigDecimal[]{
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(200),
//                BigDecimal.valueOf(300),
//                BigDecimal.valueOf(500),
        };

        for (BigDecimal limit : limits) {
            if(limit.compareTo(amount)==0){
                return;
            }
        }

        throw new IllegalArgumentException("充值金额限制：10元，20元，30元，50元，100元，200元");
    }
}



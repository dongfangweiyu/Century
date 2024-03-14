package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 广州发财 接口
 * 与第三方对接的具体实现
 */
public class FaCaiInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();
        String userUnqueNo= NumberUtil.generateCharacterString(6);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("identification", appId);//
        params.put("price", payOrder.getAmount().multiply(BigDecimal.valueOf(100)));
        params.put("type", payChannel.getPayInterfaceType());// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("return_url", payOrder.getReturnUrl());// 支付结果URL
        params.put("orderid", payOrder.getOrderNo());//
        params.put("orderuid", userUnqueNo);
        params.put("goodsname", payOrder.getOrderDesc());//
        params.put("key",getM5KeyCreate(payOrder.getOrderDesc(),appId,params.get("notify_url").toString(),
                payOrder.getOrderNo(),params.get("orderuid").toString(),params.get("price").toString(),params.get("return_url").toString(),token,payChannel.getPayInterfaceType()));//业务流水号，18位长度以上的随机字符串

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("FaCaiInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==200){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("data"));
            }else{
                String  data=jsonObject.getString("message");
                return new ApiResult().fail(data);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        try {
            request.setCharacterEncoding("utf-8");
        } catch (UnsupportedEncodingException e) {
            return "FAIL";
        }

        Map<String, Object> paramMap = getRequestParams(request);
        String bill_no=(String) paramMap.get("bill_no");
        String orderid=(String) paramMap.get("orderid");
        String price=(String) paramMap.get("price");
        String actual_price=(String) paramMap.get("actual_price");
        String orderuid=(String) paramMap.get("orderuid");
        String type=(String) paramMap.get("type");
        String poundage=(String) paramMap.get("poundage");
        String identification=(String) paramMap.get("identification");
        String goodsname=(String) paramMap.get("goodsname");
        String pay_status=(String) paramMap.get("pay_status");
        String sign=(String) paramMap.get("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderid);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(price)/100))!=0){
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

      //  String appId=config.getAppId();
        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String signKey=getM5KeyNotify(actual_price,identification,bill_no,goodsname,orderid,orderuid,pay_status,poundage,payOrder.getAmount().multiply(BigDecimal.valueOf(100)).toPlainString(),token,type);
            if (sign.equals(signKey)) {
                if(pay_status.equals("1")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return  "{\"code\":\"200\"}";
                    }
                    return "订单已完成,请结束回调";
                }
                return "支付失败";
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

        String qrcode=jsonObject.getString("qrcode");
        long end_time=jsonObject.getLongValue("end_time");
        BigDecimal amount=jsonObject.getBigDecimal("actual_price");
        int payType=jsonObject.getIntValue("type");

        if(System.currentTimeMillis()>=end_time*1000){
            throw new IllegalArgumentException("订单已过期");
        }

        model.addAttribute("qrcode",qrcode);
        model.addAttribute("endTime", end_time*1000);
        model.addAttribute("serverTime",System.currentTimeMillis());
        model.addAttribute("orderNo",payOrder.getOrderNo());
        model.addAttribute("amount",amount.divide(BigDecimal.valueOf(100)).toPlainString());
        model.addAttribute("payType",payType==1?"微信":"支付宝");
        model.addAttribute("payTypeIcon",payType==1?"/static/images/wechat.png":"/static/images/alipay.png");
        if(payType==1){

        }else{
            model.addAttribute("jumpBtn",qrcode);//跳转按钮
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
    private static String getM5KeyCreate(String goodsname , String identification , String notify_url ,
                                   String orderid ,String orderuid ,String price , String return_url , String token ,String type) {

        StringBuffer sb=new StringBuffer();
        sb.append("goodsname="+goodsname);
        sb.append("&identification="+identification);
        sb.append("&notify_url="+notify_url);
        sb.append("&orderid="+orderid);
        sb.append("&orderuid="+orderuid);
        sb.append("&price="+price);
        sb.append("&return_url="+return_url);
        sb.append("&token="+token);
        sb.append("&type="+type);

        logger.info("YiDaoInterface创建订单签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return md5Value;
    }

    /**
     *  回调签名
     * @param actual_price 实付金额
     * @param appId 应用key
     * @param bill_no 第三方订单号
     * @param goodsname 商品描述
     * @param orderid 商户订单号
     * @param orderuid 客户订单标识
     * @param pay_status 订单状态
     * @param poundage 手续费
     * @param price 订单金额
     * @param token 密钥
     * @param type 通道类型
     * @return
     */
    private static String getM5KeyNotify(String actual_price ,String appId, String bill_no ,String goodsname, String orderid  ,
                                         String orderuid  ,String pay_status,String poundage,String price  ,String token ,String type) {

        StringBuffer sb=new StringBuffer();
        sb.append("actual_price="+actual_price);
        sb.append("&identification="+appId);
        sb.append("&bill_no="+bill_no);
        sb.append("&goodsname="+goodsname);
        sb.append("&orderid="+orderid);
        sb.append("&orderuid="+orderuid);
        sb.append("&pay_status="+pay_status);
        sb.append("&poundage="+poundage);
        sb.append("&price="+price);
        sb.append("&token="+token);
        sb.append("&type="+type);

        logger.info("FaCaiInterface异步通知签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return md5Value;
    }

}



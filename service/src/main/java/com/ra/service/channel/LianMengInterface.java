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
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * http://www.lianmeng188.cn/merchant/#/payment 接口
 * 与联盟第三方对接的具体实现
 */
public class LianMengInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("identification", appId);//
        params.put("price", payOrder.getAmount().multiply(BigDecimal.valueOf(100)));
        params.put("type", payChannel.getPayInterfaceType());// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("return_url", payOrder.getReturnUrl());// 支付结果URL
        params.put("orderid", payOrder.getOrderNo());//
        params.put("orderuid", "");
        params.put("goodsname", payOrder.getOrderDesc());//
        params.put("format", "json");//
        params.put("key",getM5KeyCreate(payOrder.getOrderDesc(),appId,params.get("notify_url").toString(),
                payOrder.getOrderNo(),params.get("orderuid").toString(),params.get("price").toString(),params.get("return_url").toString(),token,payChannel.getPayInterfaceType()));//业务流水号，18位长度以上的随机字符串

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("YiDaoInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==200){
                    JSONObject data = jsonObject.getJSONObject("data");
                    return new ApiResult().ok("订单创建成功").inject(data.getString("cashier"));
            }else{
                String  data=jsonObject.getString("data");
                return new ApiResult().fail(data);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String bill_no=request.getParameter("bill_no");
        String orderid=request.getParameter("orderid");
        String price=request.getParameter("price");
        String actual_price=request.getParameter("actual_price");
        String orderuid=request.getParameter("orderuid");
        String key=request.getParameter("key");
        String goodsname=request.getParameter("goodsname");

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

        String appId=config.getAppId();
        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            String sign=getM5KeyNotify(actual_price,bill_no,orderid,orderuid,payOrder.getAmount().multiply(BigDecimal.valueOf(100)).toPlainString(),token);
            if (key.equals(sign)) {
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
        return "null";
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
    private static String getM5KeyNotify(String actual_price  , String bill_no  , String orderid  ,
                                         String orderuid  ,String price  ,String token ) {

        StringBuffer sb=new StringBuffer();
        sb.append("actual_price="+actual_price);
        sb.append("&bill_no="+bill_no);
        sb.append("&orderid="+orderid);
        sb.append("&orderuid="+orderuid);
        sb.append("&price="+price);
        sb.append("&token="+token);

        logger.info("YiDaoInterface异步通知签名加密源串："+sb.toString());
        String md5Value = EncryptUtil.encodeMD5(sb.toString()).toLowerCase();
        return md5Value;
    }

}



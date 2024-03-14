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
import java.net.URLEncoder;
import java.util.*;

/**
 * http://www.arya68.com/userweb/index.html 接口
 * 与ARYA淘宝红包的第三方对接的具体实现
 */
public class ARYAInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws UnsupportedEncodingException {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();
        Long timestamp=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("uid", appId);//
        params.put("money",payOrder.getAmount().setScale(2).toPlainString());
        params.put("channel", payChannel.getPayInterfaceType());// 支付方式
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("returnUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("outTradeNo", payOrder.getOrderNo());//

        params.put("timestamp", timestamp);
        params.put("goodsName", payOrder.getOrderDesc());
        Map<String,String> paramMap4Sign = new HashMap<>();
        paramMap4Sign.put("token",token);
        paramMap4Sign.put("uid",appId);
        paramMap4Sign.put("timestamp",params.get("timestamp").toString());
        paramMap4Sign.put("channel", payChannel.getPayInterfaceType());
        paramMap4Sign.put("money",payOrder.getAmount().setScale(2).toPlainString());
      paramMap4Sign.put("goodsName",payOrder.getOrderDesc());
        paramMap4Sign.put("notifyUrl",params.get("notifyUrl").toString());
        paramMap4Sign.put("returnUrl",params.get("returnUrl").toString());
        paramMap4Sign.put("outTradeNo",payOrder.getOrderNo());

        String signStr = EncryptUtil.encodeMD5(formatUrlMap(paramMap4Sign,false)).toUpperCase();
        params.put("sign", signStr);
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("ARYAInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==0){
                JSONObject jsonDataObject = JSON.parseObject(jsonObject.getString("data"));
                    return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonDataObject.getString("payUrl"));
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
        String outTradeNo=request.getParameter("outTradeNo");
        String realMoney=request.getParameter("realMoney");
        String notifySign=request.getParameter("sign");
        Map<String, Object> requestParams = getRequestParams(request);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(outTradeNo);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(realMoney)))!=0){
            return "金额不对";
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "该通道配置不存在";
        }

        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        if(config==null){
            return "该接口未存在";
        }
        boolean b= verifyIP(config,request);
        if(!b){
            return "回调IP不正确";
        }
        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");
            Map<String,String> paramMap4Sign = new HashMap<>();
            paramMap4Sign.put("token",token);
            paramMap4Sign.put("uid",request.getParameter("uid"));
            paramMap4Sign.put("channel",request.getParameter("channel"));
            paramMap4Sign.put("tradeNo",request.getParameter("tradeNo"));
            paramMap4Sign.put("money",request.getParameter("money"));
            paramMap4Sign.put("realMoney",request.getParameter("realMoney"));
            paramMap4Sign.put("outTradeNo",request.getParameter("outTradeNo"));

            String sign = EncryptUtil.encodeMD5(formatUrlMap(paramMap4Sign,false)).toUpperCase();
            if (sign.equals(notifySign)) {
                    //如果验签成功
                    if (payOrder.getStatus() == OrderStatusEnum.PROCESS) {
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder, null);//更新为成功订单
                        return "SUCCESS";
                    }
            }
            return "验签回调失败";
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


    public static String formatUrlMap(Map<String, String> paraMap, boolean urlEncode) throws UnsupportedEncodingException {
        String buff;
        Map<String, String> tmpMap = paraMap;
        List<Map.Entry<String, String>> infoIds = new ArrayList<>(tmpMap.entrySet());
        // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
        Collections.sort(infoIds, Comparator.comparing(o -> (o.getKey())));
        // 构造URL 键值对的格式
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> item : infoIds) {
            if (!StringUtils.isEmpty(item.getKey())) {
                String key = item.getKey();
                String val = item.getValue();
                if (urlEncode) {
                    val = URLEncoder.encode(val, "utf-8");
                }
                buf.append(key + "=" + val);
                buf.append("&");
            }
        }
        buff = buf.toString();
        if (buff.isEmpty() == false) {
            buff = buff.substring(0, buff.length() - 1);
        }
        return buff;
    }

}



package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.common.utils.SignUtils;
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
 * 本四方自身接口
 */
public class RAInterface extends CommonPayInterface{

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道缺少接口配置...");

        String appId=config.getAppId();
        String secret=config.getSecret();
        String json=config.getJson();

        String payCode=payChannel.getPayInterfaceType();//默认定义了4种自定义类型，
        try{
            JSONObject jsonConfig = JSONObject.parseObject(json);
            payCode = jsonConfig.getString(payCode);//通过这四种类型，去匹配第三方的真实类型
            Assert.hasText(payCode,"自定义JSON配置错误...");
        }catch (Exception e){
            throw new IllegalArgumentException("自定义JSON配置错误...");
        }

        long timestamp=System.currentTimeMillis();
        String nonceStr = NumberUtil.generateCharacterString(18).toUpperCase();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("appId",appId);
        params.put("amount",payOrder.getAmount());
        params.put("outOrderNo",payOrder.getOrderNo());
        params.put("orderDesc",payOrder.getOrderDesc());
        params.put("timestamp",timestamp);
        params.put("nonceStr",nonceStr);
        params.put("returnUrl",payOrder.getReturnUrl());
        params.put("notifyUrl",IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");
        params.put("signature",SignUtils.generateCreateSign(payOrder.getOrderNo(),payOrder.getAmount(),payCode,payOrder.getAttach()
                ,appId, timestamp,nonceStr, secret));
        params.put("payCode",payCode);
        params.put("attach",payOrder.getAttach());

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("RAInterface:创建订单返回JSON："+result);

        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==1){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("data"));
            }else{
                String  data=jsonObject.getString("msg");
                return new ApiResult().fail(data);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");
        Map<String, Object> paramMap = getRequestParams(request);
        String orderNo=paramMap.get("orderNo").toString();
        String outOrderNo=paramMap.get("outOrderNo").toString();
        String signature=paramMap.get("signature").toString();
        String payCode=paramMap.get("payCode").toString();
        BigDecimal amount=BigDecimal.valueOf(Integer.parseInt(paramMap.get("amount").toString()));
        String nonceStr=paramMap.get("nonceStr").toString();
        String timestamp=paramMap.get("timestamp").toString();
        String attach=paramMap.get("attach").toString();

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(outOrderNo);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(amount)!=0){
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
            String md5sign=SignUtils.generateCreateSign(outOrderNo,amount,payCode,attach,appId,Long.parseLong(timestamp),nonceStr,secret);
            if (signature.equals(md5sign)) {
                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "SUCCESS";
                }
                return "订单状态不为待支付";
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
}

package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.XmlUtils;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * new MC 接口
 * 与MC第三方对接的具体实现
 */
public class MCInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("shid", appId);//
        params.put("orderid", payOrder.getOrderNo());//
        params.put("amount", payOrder.getAmount());
        params.put("sign", getSign(params,secret));
        params.put("pay", payChannel.getPayInterfaceType());// 支付方式
        params.put("returnurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("status").equalsIgnoreCase("1")){
                return new ApiResult().ok("订单创建成功！").inject(jsonObject.getString("message"));
            }
            return new ApiResult().fail(jsonObject.getString("message"));
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String body = getBody(request);
        logger.info("XML数据为："+body);
        Map<String, Object> xmlBodyContext = XmlUtils.getXmlBodyContext(body);
        String shid=xmlBodyContext.get("shid").toString();
        String amount=xmlBodyContext.get("amount").toString();
        String orderid=xmlBodyContext.get("orderid").toString();
        String status=xmlBodyContext.get("status").toString();
        String sign=xmlBodyContext.get("pay_md5sign").toString();

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderid);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)))!=0){
            return "金额不对";
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "paychannel is null";
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
            xmlBodyContext.remove("pay_md5sign");
            // 延签
            // 验证是不是支付网关发来的异步通知
            String md5sign=getSign(xmlBodyContext,secret);
            if (sign.equals(md5sign)) {
                if(status.equals("1")){
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
        return "order";
    }

}



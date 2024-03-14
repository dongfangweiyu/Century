package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.constant.RedisConstant;
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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * http://www.ycqhpay.com/user_Index_index.html 接口
 * 与永川支付第三方对接的具体实现
 */
public class YongChuanPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pay_memberid", appId);//
        params.put("pay_orderid", payOrder.getOrderNo());//
        params.put("pay_applydate", sdf.format(System.currentTimeMillis()));//DateUtil.dateToDateTime(payOrder.getCreateTime())
        params.put("pay_bankcode", payChannel.getPayInterfaceType());// 支付方式
        params.put("pay_notifyurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("pay_callbackurl", payOrder.getReturnUrl());// 支付结果URL
        params.put("pay_amount", payOrder.getAmount());
        params.put("pay_md5sign", getSign(params,secret));//
        params.put("pay_attach","");
        params.put("pay_productname","");
        params.put("pay_productnum","");
        params.put("pay_productdesc","");
        params.put("pay_producturl","");
        params.put("pay_type","2");
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("YongChuanPayInterface:创建订单返回JSON："+result);
        //{"status":"error","msg":"用户未分配通道,暂时无法连接!","data":[]}
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getString("retCode").equalsIgnoreCase("SUCCESS")){
                return new ApiResult().ok("订单创建成功").inject(jsonObject.getString("payUrl"));
            }else{
                return new ApiResult().fail("通道下单失败，请联系客服");
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String orderid=request.getParameter("orderid");
        String amount=request.getParameter("amount");
        String datetime=request.getParameter("datetime");
        String returncode=request.getParameter("returncode");
        String transaction_id = request.getParameter("transaction_id");
        String attach=request.getParameter("attach");
        String sign=request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderid);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(amount)))!=0){
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
        String secret=config.getSecret();
        try{
            Map<String,Object> signMap=new HashMap<>();
            signMap.put("memberid",appId);
            signMap.put("orderid",orderid);
            signMap.put("amount",amount);
            signMap.put("transaction_id",transaction_id);
            signMap.put("datetime",datetime);
            signMap.put("returncode",returncode);
            signMap.put("attach",attach);
            // 延签
            // 验证是不是支付网关发来的异步通知
            String md5sign=getSign(signMap,secret);
            if (sign.equals(md5sign)) {
                if(returncode.equals("00")){
                    //如果验签成功
                    if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder,null);//更新为成功订单
                        return "OK";
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



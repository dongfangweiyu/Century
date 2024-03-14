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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * https://hd.haidaoc.xyz/Hdc_2020.php接口
 * 与海盗第三方对接的具体实现
 */
public class HaiDaoInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("mch_id", appId);//
        params.put("money",payOrder.getAmount().setScale(2).toPlainString());
        params.put("ptype", Integer.parseInt(payChannel.getPayInterfaceType()));// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("format", "page");// 支付结果URL
        params.put("order_sn", payOrder.getOrderNo());//
        params.put("goods_desc", "购买商品");
        params.put("client_ip", "127.0.0.1");
        params.put("time",getUTCTimeStr());//
        params.put("sign",getSign(params,token).toLowerCase());
        params.put("createOrderUrl",config.getCreateOrderUrl());//与接口无关，业务逻辑自己加的
        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()),JSON.toJSONString(params),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String sh_order=request.getParameter("sh_order");
        String pt_order=request.getParameter("pt_order");
        String money=request.getParameter("money");
        String time=request.getParameter("time");
        String notifySign=request.getParameter("sign");
        Map<String, Object> requestParams = getRequestParams(request);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(sh_order);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(money)))!=0){
            return "金额不对";
        }
        PayChannel payChannel = payChannelRepository.findById(payOrder.getPayChannelId().longValue());
        if(payChannel==null){
            return "未配置该订单通道";
        }

        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        if(config==null){
            return "该通道接口不存在";
        }
        boolean b= verifyIP(config,request);
        if(!b){
            return "回调IP不正确";
        }
        if(config.getBalanceLimit().compareTo(BigDecimal.ZERO)==1&&config.getMoney().compareTo(config.getBalanceLimit())==1){
            return "通道余额超出限制，请联系客服下发";
        }
        String appId=config.getAppId();
        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");
            String sign=getSign(requestParams,token).toLowerCase();
            if (sign.equals(notifySign)) {
                //如果验签成功
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("mch_id", appId);//
                params.put("out_order_sn",sh_order);
                params.put("time",time);//
                params.put("sign",getSign(params,token).toLowerCase());
                String result = HttpUtil.doPOST("https://hd.haidaoc.xyz/?c=Pay&a=query", params);
                logger.info("海盗:查询订单结果返回的JSON："+result);
                if(!StringUtils.isEmpty(result)){
                    JSONObject jsonObject = JSON.parseObject(result);
                    if(jsonObject.getIntValue("code")==1){
                        JSONObject jsonData = jsonObject.getJSONObject("data");
                        if(jsonData.getIntValue("status")==9){//如果查询的状态是已支付
                            //如果查询的金额也一致
                            if(jsonData.getBigDecimal("money").compareTo(payOrder.getAmount())==0){

                                if (payOrder.getStatus() == OrderStatusEnum.PROCESS) {

                                    //如果订单还在处理中
                                    payOrderService.successOrder(payOrder, null);//更新为成功订单
                                    return "success";
                                }
                                return "订单状态不是待支付";
                            }
                            return "金额不符";
                        }
                        return "支付失败";
                    }
                    return "通道查询不出该订单信息";
                }
                 return "查询订单结果有误";
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
        HashMap hashMap = JSON.parseObject(jsonData, HashMap.class);
        model.addAttribute("data",hashMap);
        model.addAttribute("createOrderUrl",hashMap.get("createOrderUrl"));
        return "jump";
    }

    public static Integer getUTCTimeStr(){
        try {
            Calendar cal = Calendar.getInstance();
            TimeZone tz = TimeZone.getTimeZone("CST");
            cal.setTimeZone(tz);
            String timestamp= String.valueOf(cal.getTimeInMillis());
            int length=timestamp.length();
            if(length>3){
                return Integer.valueOf(timestamp.substring(0,length-3));
            }else{
                return 0;
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        }
        return 0;
    }

}



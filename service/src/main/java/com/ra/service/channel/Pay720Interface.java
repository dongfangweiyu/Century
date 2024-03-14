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
 * http://www.720pays.com/doc 接口
 * 与720pay第三方对接的具体实现
 */
public class Pay720Interface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("customerid", appId);//
        params.put("sdorderno", payOrder.getOrderNo());//
        params.put("paytype", payChannel.getPayInterfaceType());// 支付方式
        params.put("notifyurl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("returnurl", payOrder.getReturnUrl());// 支付结果URL
        params.put("total_fee", payOrder.getAmount().setScale(2).toPlainString());
        params.put("payuser_account","1");
        params.put("get_code","1");
        params.put("remark","");
        params.put("version","1.0");
        params.put("sign", generalSign("1.0",appId,params.get("total_fee").toString(),payOrder.getOrderNo(),params.get("notifyurl").toString(),params.get("returnurl").toString(),secret));//
        //{"status":1,"msg":"成功订单","sdorderno":"商户订单号","total_fee":"订单金额","sdpayno":"平台订单号"}
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("Pay720Interface下单返回："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("status")==1){
                return new ApiResult().ok("订单创建成功！").inject(jsonObject.getString("payurl"));
            }
            return new ApiResult().fail("订单创建失败！");
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到支付结果的异步通知...");
        String customerid=request.getParameter("customerid");
        String sdorderno=request.getParameter("sdorderno");
        String total_fee=request.getParameter("total_fee");
        String sdpayno=request.getParameter("sdpayno");
        String status=request.getParameter("status");
        String paytype = request.getParameter("paytype");
        String remark=request.getParameter("remark");
        String sign=request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(sdorderno);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(total_fee)))!=0){
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
            String md5sign=notifySign(customerid,status,sdpayno,sdorderno,total_fee,secret);
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
        return null;
    }


    /**
     * 生成签名
     * @return
     */
    private String generalSign(String version,String customerid,String total_fee,String sdorderno,String notifyurl,String returnurl,String key){
        String str="version={version}&customerid={customerid}&total_fee={total_fee}&sdorderno={sdorderno}&notifyurl={notifyurl}&returnurl={returnurl}&{apikey}";
        str=str.replace("{version}",version);
        str=str.replace("{customerid}",customerid);
        str=str.replace("{total_fee}",total_fee);
        str=str.replace("{sdorderno}",sdorderno);
        str=str.replace("{notifyurl}",notifyurl);
        str=str.replace("{returnurl}",returnurl);
        str=str.replace("{apikey}",key);
        logger.info("签名源串="+str);
        String s = EncryptUtil.encodeMD5(str).toLowerCase();
        return s;
    }

    /**
     * 异步通知签名方法
     * @param customerid
     * @param status
     * @param sdpayno
     * @param total_fee
     * @param key
     * @return
     */
    private String notifySign(String customerid,String status,String sdpayno,String sdorderno,String total_fee,String key){
        String str="customerid={customerid}&status={status}&sdpayno={sdpayno}&sdorderno={sdorderno}&total_fee={total_fee}&{apikey}";
        str=str.replace("{customerid}",customerid);
        str=str.replace("{status}",status);
        str=str.replace("{sdpayno}",sdpayno);
        str=str.replace("{sdorderno}",sdorderno);
        str=str.replace("{total_fee}",total_fee);
        str=str.replace("{apikey}",key);
        logger.info("签名源串="+str);
        String s = EncryptUtil.encodeMD5(str).toLowerCase();
        return s;

    }
}



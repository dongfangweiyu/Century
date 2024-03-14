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

import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;

/**
 * http://www.yunzhifu8.top/ht.php#/User/rsaset 接口
 * 与云支付第三方对接的具体实现
 */
public class YunPayInterface extends CommonPayInterface {

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
        params.put("format", "json");// 支付结果URL
        params.put("order_sn", payOrder.getOrderNo());//
        params.put("goods_desc", "recharge");
        params.put("client_ip", "127.0.0.1");
        params.put("time",getUTCTimeStr());//
//        params.put("sign",getM5KeyCreate(appId,payChannel.getPayInterfaceType(),payOrder.getOrderNo(),params.get("money").toString(),params.get("goods_desc").toString(),
//                params.get("client_ip").toString(),params.get("format").toString(),params.get("notify_url").toString(),
//                params.get("time").toString(),token));//业务流水号，18位长度以上的随机字符串
        params.put("sign",getSign(params,token).toLowerCase());
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("YunPayInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==1){
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    //String pay_url = "http://www.yunzhifu8.top/?c=Pay&a=info&osn="+jsonData.getString("pay_url");
                    return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonData.getString("pay_url"));
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
        String sh_order=request.getParameter("sh_order");
        String pt_order=request.getParameter("pt_order");
        String money=request.getParameter("money");
        String time=request.getParameter("time");
        String status=request.getParameter("status");
        String notifySign=request.getParameter("sign");
        Map<String, Object> requestParams = getRequestParams(request);

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(sh_order);
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

        String token=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");
            String sign=getSign(requestParams,token).toLowerCase();
            if (sign.equals(notifySign)) {
                if(status.equals("success")) {
                    //如果验签成功
                    if (payOrder.getStatus() == OrderStatusEnum.PROCESS) {

                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder, null);//更新为成功订单
                        return "success";
                    }
                }else{
                    return "支付失败";
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
        return "order";
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



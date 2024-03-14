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
 * http://www.juhezhifuu.com/index.html#/home 接口
 * 与第三方对接的具体实现
 */
public class JuhezhifuuInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        long time=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("total_fee", payOrder.getAmount());// 金额
        params.put("service", "placeOrder");//
        params.put("paytype", payChannel.getPayInterfaceType());// 支付方式
        params.put("notify_url", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("mch_id", appId);// 商户ID
        params.put("out_trade_no", payOrder.getOrderNo());
        params.put("time", time);// 时间戳

        params.put("sign", generateCreateSign(params, secret));// 签名

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        logger.info("JuhezhifuuInterface下单返回结果："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==0){
                JSONObject data = jsonObject.getJSONObject("data");
                String pay_url = data.getString("pay_url");
                return new ApiResult().ok(jsonObject.getString("msg")).inject(pay_url);
            }
            return new ApiResult().fail(jsonObject.getString("msg"));
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");

        Map<String,Object> paramMap = request2payResponseMap(request, new String[]{
                "mch_id", "out_trade_no", "total_fee", "status", "pay_time", "realmoney", "charge",
                "chargemoney", "discount", "create_time"
        });

        String out_trade_no=request.getParameter("out_trade_no");
        String status=request.getParameter("status");
        String sign=request.getParameter("sign");
//        Double total_fee=Double.parseDouble(request.getParameter("total_fee"));

        if(!status.equalsIgnoreCase("0")){
            return "FAIL";
        }

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(out_trade_no);
        if(payOrder==null){
            return "订单不存在";
        }
//        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(total_fee))!=0){
//            return "FAIL";
//        }

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
        String serect=config.getSecret();
        try{
            paramMap.put("mch_id",appId);
            // 延签
            // 验证是不是支付网关发来的异步通知
            String signature=generateCreateSign(paramMap,serect);
            if (signature.equals(sign)) {

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
    private static String generateCreateSign(Map<String, Object> params, String secret) {

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder();
        for (String key : keys) {
            requestUrl.append(key).append("=");
            requestUrl.append(params.get(key));
            requestUrl.append("&");
        }

        requestUrl.append("key="+secret);
        logger.info("签名源串："+requestUrl.toString());
        String orginSignature = EncryptUtil.encodeMD5(requestUrl.toString()).toUpperCase();
        logger.info("签名："+orginSignature);
        return orginSignature;
    }

    private static Map<String, Object> request2payResponseMap(HttpServletRequest request, String[] paramArray) {
        Map<String, Object> responseMap = new HashMap<>();
        for (int i = 0;i < paramArray.length; i++) {
            String key = paramArray[i];
            String v = request.getParameter(key);
            if (v != null) {
                responseMap.put(key, v);
            }
        }
        return responseMap;
    }

}

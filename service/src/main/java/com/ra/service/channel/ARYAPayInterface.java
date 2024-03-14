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
 * ARYA 接口
 * 与第三方http://39.98.150.18/userweb/index.html对接的具体实现
 */
public class ARYAPayInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String secret=config.getSecret();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("uid",appId);//您的商户唯一标识，注册后在设置里获得。
        params.put("money", payOrder.getAmount().setScale(2).toPlainString());// 金额
        params.put("channel", "alipay_zk");// 支付通道
        params.put("outTradeNo", payOrder.getOrderNo());
        params.put("notifyUrl", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("returnUrl", payOrder.getReturnUrl());// 支付结果URL
        params.put("goodsName","");//
        params.put("outUserId","");//
        params.put("outBody","");//
        params.put("channelAccount","");//
        params.put("payMode",payChannel.getPayInterfaceType());//支付模式
        params.put("bonusRate", "");//
        params.put("timestamp", System.currentTimeMillis());// 时间戳
        params.put("sign", getSign(params,secret));//签名

        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), params);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==0){
                JSONObject data=jsonObject.getJSONObject("data");
                return new ApiResult().ok(jsonObject.getString("msg")).inject(data.getString("payUrl"));
            }
            return new ApiResult().fail(jsonObject.getString("msg"));
        }
        return new ApiResult().fail("请求出码服务器超时...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");


        Map<String, Object> requestParams = getRequestParams(request);
        String realMoney=request.getParameter("realMoney");
        String outTradeNo=request.getParameter("outTradeNo");
        String sign=request.getParameter("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(outTradeNo);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(realMoney)))!=0){
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
        String serect=config.getSecret();
        try{
            // 延签
            // 验证是不是支付网关发来的异步通知
            requestParams.remove("sign");

            String signature=getSign(requestParams,serect);
            if (signature.equals(sign)) {

                //如果验签成功
                if(payOrder.getStatus()== OrderStatusEnum.PROCESS){

                    //如果订单还在处理中
                    payOrderService.successOrder(payOrder,null);//更新为成功订单
                    return "SUCCESS";
                }
                return "订单状态不是待支付";
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


    private String getSortJson(JSONObject obj){
        SortedMap map = new TreeMap();
        Set<String> keySet = obj.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            Object vlaue = obj.get(key);
            map.put(key, vlaue);
        }
        return JSONObject.toJSONString(map);
    }

    /**
     * 创建订单签名
     * @description 生成参数签名
     * @return 回调参数签名
     */
    @Override
    protected String getSign(Map<String,Object> map, String key) {

        ArrayList<String> list = new ArrayList<String>();
        for(Map.Entry<String,Object> entry:map.entrySet()){
            if(null != entry.getValue() && !"".equals(entry.getValue())){
                if(entry.getValue() instanceof JSONObject) {
                    list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
                }else {
                    list.add(entry.getKey() + "=" + entry.getValue() + "&");
                }
            }
        }
        list.add("token="+key+"&");
        int size = list.size();
        String [] arrayToSort = list.toArray(new String[size]);
        Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < size; i ++) {
            sb.append(arrayToSort[i]);
        }
        String result = sb.substring(0,sb.length()-1);

        logger.info("签名加密源串："+result);
        result = EncryptUtil.encodeMD5(result, "UTF-8").toUpperCase();
        logger.info("签名加密结果："+result);
        return result;
    }
}

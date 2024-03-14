package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * http://hyywhcy.com/payapi/Index/api 接口
 * 与雄狮支付的第三方对接的具体实现
 */
public class XiongShiInterface extends CommonPayInterface {

    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) throws UnsupportedEncodingException {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道接口配置有误...");

        String appId=config.getAppId();
        String token=config.getSecret();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long timestamp=System.currentTimeMillis();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("merchant_order_uid", appId);//
        params.put("merchant_order_money",payOrder.getAmount().setScale(2).toPlainString());
        params.put("merchant_order_channel", payChannel.getPayInterfaceType());// 支付方式
        params.put("merchant_order_callbak_confirm_duein", IpUtil.getHost(request)+"/pay/"+payChannel.getPayInterface().name()+"/notify");// 异步通知URL
        params.put("merchant_order_date", sdf.format(timestamp));// 支付结果URL
        params.put("merchant_order_sn", payOrder.getOrderNo());//

        Map<String,String> paramMap4Sign = new HashMap<>();
        paramMap4Sign.put("merchant_order_uid",appId);
        paramMap4Sign.put("merchant_order_money",payOrder.getAmount().setScale(2).toPlainString());
        paramMap4Sign.put("merchant_order_channel", payChannel.getPayInterfaceType());
        paramMap4Sign.put("merchant_order_callbak_confirm_duein", params.get("merchant_order_callbak_confirm_duein").toString());//.replaceAll("/","\\\\/")
        paramMap4Sign.put("merchant_order_date",sdf.format(timestamp));
        paramMap4Sign.put("merchant_order_sn",payOrder.getOrderNo());

        String str=formatUrlMap(paramMap4Sign,false)+"&apikey="+token;
        String signStr = EncryptUtil.encodeMD5(str.toUpperCase()).toLowerCase();
        params.put("merchant_order_sign", signStr);
        String result = HttpUtil.doPOST(config.getCreateOrderUrl(), JSON.toJSONString(params));
        logger.info("XiongShiInterface:创建订单返回JSON："+result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSON.parseObject(result);
            if(jsonObject.getIntValue("code")==200){
                JSONObject jsonDataObject = JSON.parseObject(jsonObject.getString("data"));
                    return new ApiResult().ok(jsonObject.getString("msg")).inject(jsonDataObject.getString("url"));
            }else{
                String  msg=jsonObject.getString("msg");
                return new ApiResult().fail(msg);
            }
        }
        return new ApiResult().fail("请求出码服务器失败...");
    }

    @Override
    public String onNotify(HttpServletRequest request) {

        logger.info("收到雄狮支付结果的异步通知...");
        String body = getBody(request);
        if(org.springframework.util.StringUtils.isEmpty(body)){
            return "回调数据为："+body;
        }
        JSONObject jsonObject = JSON.parseObject(body);
        String code=jsonObject.getString("code");
        String msg=jsonObject.getString("msg");
        String data=jsonObject.getString("data");
        JSONObject jsonObjectData = JSON.parseObject(data);
        String order_id=jsonObjectData.getString("order_id");
        String order_money=jsonObjectData.getString("order_money");
        String merchant_order_sn=jsonObjectData.getString("merchant_order_sn");
        String sign=jsonObjectData.getString("sign");

        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(merchant_order_sn);
        if(payOrder==null){
            return "订单不存在";
        }
        if(payOrder.getAmount().compareTo(BigDecimal.valueOf(Double.parseDouble(order_money)))!=0){
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
            Map<String,String> paramMap4Sign = new HashMap<>();
            paramMap4Sign.put("order_id",order_id);
            paramMap4Sign.put("order_money",order_money);
            paramMap4Sign.put("merchant_order_sn",merchant_order_sn);
            String str=formatUrlMap(paramMap4Sign,false)+"&apikey="+token;
            String signStr = EncryptUtil.encodeMD5(str.toUpperCase()).toLowerCase();
            if (sign.equals(signStr)) {
                if(code.equals("200")) {
                    //如果验签成功
                    if (payOrder.getStatus() == OrderStatusEnum.PROCESS) {
                        //如果订单还在处理中
                        payOrderService.successOrder(payOrder, null);//更新为成功订单
                        return "SUCCESS";
                    }
                    return "订单已完成,请结束回调";
                }
                return "支付失败";
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



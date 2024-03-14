package com.ra.service.component;

import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.common.utils.SignUtils;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.PayOrder;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单匹配用户
 */
@Component
public class PayOrderComponent {

    private static final Logger logger= LoggerFactory.getLogger(PayOrderComponent.class);
    @Autowired
    private PayOrderService payOrderService;
    @Autowired
    private MerchantInfoService merchantInfoService;

    /**
     * 异步回调
     * @Async注解用于异步执行
     * @param order
     */
    @Async
    public void notifyOrder(PayOrder order,String statusDesc) {
        try{
            MerchantInfo merchantInfo = merchantInfoService.getRepository().findByUserId(order.getMerchantUserId());
            Assert.notNull(merchantInfo,"商户信息不存在,回调失败");

            long timestamp=System.currentTimeMillis();
            String nonceStr = NumberUtil.generateCharacterString(18).toUpperCase();

            String signature = SignUtils.generateCreateSign(order.getOutOrderNo(),order.getAmount(),order.getPayCode(),order.getAttach(),
                    merchantInfo.getAppId(),timestamp,nonceStr, merchantInfo.getSecret()); //参数签名

            Map<String,Object> params=new HashMap<>();
            params.put("orderNo",order.getOrderNo());
            params.put("outOrderNo",order.getOutOrderNo());
            params.put("amount", order.getAmount().toPlainString());
            params.put("payCode",order.getPayCode());
            params.put("attach",order.getAttach());
            params.put("timestamp", timestamp);
            params.put("nonceStr", nonceStr);
            params.put("signature",signature);

            logger.info("**************【异步通知执行】 url: " + order.getNotifyUrl() + "***************");
            String s = HttpUtil.doPOST(order.getNotifyUrl(), params);
            if(!StringUtils.isEmpty(s)&&s.equalsIgnoreCase("SUCCESS")){
                payOrderService.getRepository().completeOrder(order.getId(),"回调成功,订单完结（"+statusDesc+"）");
            }else if(order.getNotifyCount()>=9){//没有回调成功
                payOrderService.getRepository().updateStatusDesc(order.getId(),"回调失败"+(order.getNotifyCount()+1)+"次,请联系商户手动上分（"+statusDesc+"）");
            }
            logger.info("**************【异步通知执行结果】 result: " + s + "***************");
        }catch (IllegalArgumentException e){
            logger.info("**************【异步通知执行结果】 status: " + e.getMessage() + "***************");
        }catch (Exception e){
            logger.info("**************【异步通知执行结果】 status: " + e.getMessage() + "***************");
            e.printStackTrace();
        }finally {
            payOrderService.getRepository().notifyOrderCount(order.getId());
        }
    }
}

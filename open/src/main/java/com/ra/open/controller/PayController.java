package com.ra.open.controller;

import com.alibaba.fastjson.JSONObject;
import com.ra.common.base.ApiResult;
import com.ra.common.component.RedisComponent;
import com.ra.common.constant.RedisConstant;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.channel.PayInterface;
import com.ra.dao.channel.PayInterfaceEnum;
import com.ra.dao.channel.PayInterfaceFactory;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.params.CreateOrderResp;
import com.ra.service.bean.params.OrderCreateParams;
import com.ra.service.bean.params.OrderQueryParams;
import com.ra.service.bean.params.OrderQueryResp;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商户对接接口
 */
@Controller
@RequestMapping("/pay/")
public class PayController {

    private final static Logger logger= LoggerFactory.getLogger(PayController.class);

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private PayOrderService payOrderService;
    @Autowired
    private PayChannelService payChannelService;

    /**
     * 查询订单
     *
     * @return
     */
    @PostMapping(value = "/query")
    @ResponseBody
    public ApiResult query(@ModelAttribute OrderQueryParams params) {

        Assert.isTrue(!StringUtils.isEmpty(params.getOrderNo())||!StringUtils.isEmpty(params.getOutOrderNo()),"参数orderNo或者outOrderNo:必须有一个不能为空");
        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且与GMT+8北京时间时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");

        ApiResult result = new ApiResult();
        if(StringUtils.isEmpty(params.getOrderNo())&&StringUtils.isEmpty(params.getOutOrderNo())){
            throw new IllegalArgumentException("orderNo或者outOrderNo不能都为空");
        }
        List<OrderQueryResp> merchantQueryOtcOrder = payOrderService.findMerchantQueryOtcOrder(params);
        return result.ok("成功查询到"+merchantQueryOtcOrder.size()+"条数据。").inject(merchantQueryOtcOrder);
    }

    /**
     * 创建订单
     *
     * @return
     */
    @PostMapping(value = "/create")
    @ResponseBody
    public ApiResult createOrder(HttpServletRequest request,@ModelAttribute OrderCreateParams params) {

        Assert.notNull(params.getAmount(),"参数amount:订单金额不能为空");
        Assert.hasText(params.getOutOrderNo(),"参数outOrderNo:商户订单号不能为空");
        Assert.hasText(params.getOrderDesc(),"参数orderDesc:订单描述信息不能为空");
        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.notNull(params.getReturnUrl(),"参数returnUrl:同步返回地址不能为空");
        Assert.hasText(params.getNotifyUrl(),"参数notifyUrl:异步回调通知地址不能为空");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");
        Assert.notNull(params.getPayCode(),"参数payCode：支付编码不能为空");
        Assert.notNull(params.getAttach(),"参数attach:附加参数不能为空");

        boolean create = ConfigFactory.getBoolean(ConfigEnum.SYSTEM_CREATEORDER);
        if(!create){
            throw new IllegalArgumentException("通道维护中,请联系客服。");
        }

        //验证商户提交的时间戳，时间戳必须在1分钟之内，网络延迟不允许超过一分钟，时区要求保持一致
        if (System.currentTimeMillis() > (params.getTimestamp() + 60 * 1000)||(params.getTimestamp()-60*1000)>System.currentTimeMillis()) {
            throw new IllegalArgumentException("参数timestamp请与网关服务器保持同一时区，GMT+8北京时间");
        }

        //判断整数
        BigDecimal intAmount = BigDecimal.valueOf(params.getAmount().intValue());
        if(params.getAmount().compareTo(intAmount)!=0){
            throw new IllegalArgumentException("仅支持整数金额");
        }

        CreateOrderResp resp = payOrderService.createOrder(params);//先创建订单
        return resp.onPay(request);//再调用第三方接口，到第三方下单
    }


    /**
     * 通知订单
     *
     * @return
     */
    @RequestMapping(value = "/{payInterface}/notify")
    @ResponseBody
    public Object notify(HttpServletRequest request, @PathVariable("payInterface") PayInterfaceEnum payInterface) {
        return PayInterfaceFactory.newInstance(payInterface).onNotify(request);
    }


    /**
     * 跳转支付页面
     *
     * @return
     */
    @GetMapping(value = "/{orderNo}")
    public String pay(Model model, @PathVariable("orderNo") String orderNo) {
        PayOrder payOrder=null;
        try{
            Assert.hasText(orderNo,"参数丢失");
            payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
            Assert.notNull(payOrder,"订单不存在");
            PayChannel payChannel = payChannelService.getRepository().findById(payOrder.getPayChannelId().longValue());
            Assert.notNull(payChannel,"订单通道不存在");
            String orderData= redisComponent.getString(RedisConstant.getOrderInfo(orderNo));
            Assert.hasText(orderData,"订单已失效");

            if(payOrder.getStatus()== OrderStatusEnum.SUCCESS){
                throw new IllegalArgumentException("请不要重复支付");
            }
            if(payOrder.getStatus()==OrderStatusEnum.FAIL){
                throw new IllegalArgumentException("订单已关闭");
            }

            PayInterface payInterface = PayInterfaceFactory.newInstance(payChannel.getPayInterface());
            return payInterface.parseData(model,payOrder,orderData);
        }catch (Exception e) {
            model.addAttribute("message", e.getMessage());
            if(payOrder!=null){
                model.addAttribute("returnUrl",payOrder.getReturnUrl());
            }
            return "error";
        }
    }

    /**
     * 检测支付是否完成
     *
     * @return
     */
    @PostMapping(value = "/{orderNo}")
    @ResponseBody
    public ApiResult check(Model model, @PathVariable("orderNo") String orderNo) {
        ApiResult result=new ApiResult();

        try{
            Assert.hasText(orderNo,"参数丢失");
            PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
            Assert.notNull(payOrder,"订单不存在");
            if(payOrder.getStatus()==OrderStatusEnum.FAIL){
                throw new IllegalArgumentException("订单已关闭");
            }
            if(payOrder.getStatus()== OrderStatusEnum.SUCCESS){
                return result.ok("支付成功").inject(payOrder.getReturnUrl());
            }

            PayChannel payChannel = payChannelService.getRepository().findById(payOrder.getPayChannelId().longValue());
            Assert.notNull(payChannel,"订单通道不存在");
            String orderData= redisComponent.getString(RedisConstant.getOrderInfo(orderNo));
            Assert.hasText(orderData,"订单已失效");

        }catch (IllegalArgumentException e){
            return result.fail(-2,e.getMessage());
        }catch (Exception e){
            return result.fail(-2,e.getMessage());
        }
        return result.fail(-1,"待支付");
    }
}

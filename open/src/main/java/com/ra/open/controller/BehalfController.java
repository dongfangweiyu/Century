package com.ra.open.controller;

import com.ra.common.base.ApiResult;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.entity.business.BehalfOrder;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.params.*;
import com.ra.service.business.BehalfOrderService;
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
 * 商户对接代付接口
 */
@Controller
@RequestMapping("/behalf/")
public class BehalfController {

    private final static Logger logger= LoggerFactory.getLogger(BehalfController.class);

    @Autowired
    private BehalfOrderService behalfOrderService;

    /**
     * 查询代付订单
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
        List<BehalfOrderQueryResp> merchantQueryOtcOrder = behalfOrderService.findMerchantQueryOtcOrder(params);
        return result.ok("成功查询到"+merchantQueryOtcOrder.size()+"条数据。").inject(merchantQueryOtcOrder);
    }

    /**
     * 查询余额
     *
     * @return
     */
    @PostMapping(value = "/balance")
    @ResponseBody
    public ApiResult balance(@ModelAttribute OrderQueryParams params) {
        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且与GMT+8北京时间时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");
        ApiResult result = new ApiResult();
        Wallet wallet=behalfOrderService.findMerchantWallet(params);
        return result.ok("余额查询成功").inject(wallet.getMoney());
    }

    /**
     * 创建代付订单
     *
     * @return
     */
    @PostMapping(value = "/create")
    @ResponseBody
    public ApiResult createOrder(HttpServletRequest request,@ModelAttribute BehalfOrderCreateParams params) {

        Assert.notNull(params.getAmount(),"参数amount:订单金额不能为空");
        Assert.hasText(params.getOutOrderNo(),"参数outOrderNo:商户订单号不能为空");
        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");
        Assert.hasText(params.getNotifyUrl(),"参数notifyUrl:异步回调通知地址不能为空");
        Assert.hasText(params.getBankCard().getBankNo(),"参数bankCard.bankNo:卡号不能为空");
        Assert.hasText(params.getBankCard().getBankName(),"参数bankCard.bankName:银行名称不能为空");
        Assert.hasText(params.getBankCard().getRealName(),"参数bankCard.realName:持卡人真实姓名不能为空");
//        Assert.hasText(params.getBankCard().getBankBranch(),"参数bankCard.bankBranch:支行信息不能为空不能为空");

        boolean create = ConfigFactory.getBoolean(ConfigEnum.SYSTEM_CREATEBEHALFORDER);
        if(!create){
            throw new IllegalArgumentException("通道维护中,请联系客服。");
        }

        //验证商户提交的时间戳，时间戳必须在1分钟之内，网络延迟不允许超过一分钟，时区要求保持一致
        if (System.currentTimeMillis() > (params.getTimestamp() + 60 * 1000)||(params.getTimestamp()-60*1000)>System.currentTimeMillis()) {
            throw new IllegalArgumentException("参数timestamp请与网关服务器保持同一时区，GMT+8北京时间");
        }

        //判断整数
        BigDecimal intAmount = params.getAmount().setScale(2,BigDecimal.ROUND_DOWN);
        if(params.getAmount().compareTo(intAmount)!=0){
            throw new IllegalArgumentException("金额仅支持2位小数");
        }

        BehalfOrder behalfOrder = behalfOrderService.createOrder(request,params);//先创建订单
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/behalf/"+ behalfOrder.getOrderNo());
    }


    /**
     * 订单状态详情
     *
     * @return
     */
    @GetMapping(value = "/{orderNo}")
    @ResponseBody
    public String orderNo(Model model, @PathVariable("orderNo") String orderNo) {
        Assert.hasText(orderNo,"参数异常");
        BehalfOrder behalfOrder = behalfOrderService.getRepository().findByOrderNo(orderNo);
        if(behalfOrder ==null){
            return "订单不存在";
        }
        return behalfOrder.getStatusDesc();
    }

//    /**
//     * 通知订单
//     *
//     * @return
//     */
//    @RequestMapping(value = "/{payInterface}/notify")
//    @ResponseBody
//    public Object notify(HttpServletRequest request, @PathVariable("payInterface") PayInterfaceEnum payInterface) {
//        return PayInterfaceFactory.newInstance(payInterface).onNotify(request);
//    }

    /**
     * 卡商查询待处理代付订单
     *
     * @return
     */
    @PostMapping(value = "/behalfQueryList")
    @ResponseBody
    public ApiResult behalfQueryList(@ModelAttribute OrderQueryParams params) {

        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且与GMT+8北京时间时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");

        ApiResult result = new ApiResult();

        List<BehalfOrderListQueryResp> behalfOrderListQueryResps = behalfOrderService.findBehalfQueryOtcOrder(params);
        return result.ok("成功查询到"+behalfOrderListQueryResps.size()+"条数据。").inject(behalfOrderListQueryResps);
    }

    /**
     * 自动确认代付订单
     *
     * @return
     */
    @PostMapping(value = "/automaticConfirmOrder")
    @ResponseBody
    public ApiResult automaticConfirmOrder(HttpServletRequest request,@ModelAttribute BehalfOrderAutomaticParams params) {

        Assert.notNull(params.getAmount(),"参数amount:订单金额不能为空");
        Assert.hasText(params.getOrderNo(),"参数orderNo:订单号不能为空");
        Assert.notNull(params.getTimestamp(),"参数timestamp:请求时间戳不能为空,且时间差不能大于60秒");
        Assert.hasText(params.getNonceStr(),"参数nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性");
        Assert.hasText(params.getAppId(),"参数appId:不能为空");
        Assert.hasText(params.getSignature(),"参数signature:签名不能为空");
        Assert.hasText(params.getAuditRequest(),"参数auditRequest:审核方式不能为空");

        //判断整数
        BigDecimal intAmount = params.getAmount().setScale(2,BigDecimal.ROUND_DOWN);
        if(params.getAmount().compareTo(intAmount)!=0){
            throw new IllegalArgumentException("金额仅支持2位小数");
        }
        if("reject".equals(params.getAuditRequest())){
            Assert.hasText(params.getRemark(),"驳回取消订单,请填写原因remark");
        }
        boolean i=behalfOrderService.behalfAutomaticConfirmOrder(request,params);
        if(i){
            return new ApiResult().ok("订单自动处理成功");
        }
        return new ApiResult().fail("自动处理失败");
    }
}

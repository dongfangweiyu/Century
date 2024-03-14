package com.ra.service.channel;

import com.alibaba.fastjson.JSON;
import com.ra.common.base.ApiResult;
import com.ra.common.bean.ExtraData;
import com.ra.common.configuration.SpringContextHolder;
import com.ra.common.constant.RedisConstant;
import com.ra.common.domain.ExtraDataInterface;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.service.bean.req.BehalfOrderReq;
import com.ra.service.bean.resp.BehalfOrderTotalVo;
import com.ra.service.business.BehalfOrderService;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

/**
 * 本四方自身的代付接口
 */
public class BehalfInterface extends CommonPayInterface implements ExtraDataInterface {


    @Override
    public ApiResult onPay(HttpServletRequest request, PayOrder payOrder, PayChannel payChannel) {
        ConfigPayInterface config = configPayInterfaceRepository.findById(payChannel.getConfigPayInterfaceId().longValue());
        Assert.notNull(config,"第三方通道缺少接口配置...");

        String appId=config.getAppId();
        String secret=config.getSecret();
        String json=config.getJson();

        BehalfBankCard behalfBankCard = behalfBankCardService.matchBehalfBankCard(payOrder.getMerchantUserId(),true);
        if(behalfBankCard==null){
            return new ApiResult().fail("创建订单失败,通道不存在、没有开启或者没有分配通道。错误代码：BEHALFBANKCARD");
        }

        ExtraData extraData=new ExtraData();
        extraData.setConvertClass(this.getClass().getName());
        extraData.setData(behalfBankCard);
        int i = payOrderService.getRepository().updateExtraData(payOrder.getId(), JSON.toJSONString(extraData));
        if(i<=0){
            throw new IllegalArgumentException("银行卡数据写入订单信息失败。");
        }

        redisComponent.set(RedisConstant.getOrderInfo(payOrder.getOrderNo()), JSON.toJSONString(behalfBankCard),60*30);
        return new ApiResult().ok("订单创建成功").inject(IpUtil.getHost(request)+"/pay/"+payOrder.getOrderNo());
    }

    @Override
    public String onNotify(HttpServletRequest request) {
        logger.info("收到支付结果的异步通知...");
        return "FAIL";
    }

    @Override
    public String parseData(Model model,PayOrder payOrder, String jsonData) throws Exception{
        long end_time=payOrder.getCreateTime().getTime()+1000*60*30;
        if(System.currentTimeMillis()>=end_time){
            throw new IllegalArgumentException("订单已过期");
        }

        BehalfOrderReq req=new BehalfOrderReq();
        req.setStatus("PROCESS");
        BehalfOrderService behalfOrderService= SpringContextHolder.getBean(BehalfOrderService.class);
        BehalfOrderTotalVo behalfOrderTotal = behalfOrderService.findByBehalfOrderTotal(req, payOrder.getMerchantUserId(), null);

        BehalfBankCard bankCard=JSON.parseObject(jsonData,BehalfBankCard.class);
        model.addAttribute("bankCard",bankCard);
        model.addAttribute("behalfOrderTotal",behalfOrderTotal);
        model.addAttribute("orderNo",payOrder.getOrderNo());
        model.addAttribute("amount",payOrder.getAmount());
        model.addAttribute("endTime", end_time);
        model.addAttribute("serverTime",System.currentTimeMillis());
        return "behalf";
    }

    @Override
    public boolean invoke(Object o) {
        PayOrder payOrder= (PayOrder)o;
        String extraDataJson = payOrder.getExtraData();
        ExtraData extraData = JSON.parseObject(extraDataJson, ExtraData.class);
        BehalfBankCard behalfBankCard = JSON.parseObject(extraData.getData().toString(), BehalfBankCard.class);
        int i = walletService.addMoney(behalfBankCard.getBehalfUserId(), payOrder.getAmount(), WalletLogEnum.BEHALF);
        if(i>0){
                BehalfBankCard bankCard = behalfBankCardService.getRepository().findById(behalfBankCard.getId().longValue());
                if(bankCard!=null) {
                    behalfBankCardService.addMoney(behalfBankCard.getId(), payOrder.getAmount(), BehalfWalletLogEnum.COMEIN);
                }
            return true;
        }
        throw new IllegalArgumentException("卡商银行卡入账失败");
    }
}

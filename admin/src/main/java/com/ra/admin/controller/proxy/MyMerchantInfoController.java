package com.ra.admin.controller.proxy;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.resp.AddMechantInfoVo;
import com.ra.service.bean.resp.MerchantInfoVo;
import com.ra.service.bean.resp.PayCode2PayChannelBean;
import com.ra.service.bean.resp.RateVo;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayCodeService;
import com.ra.service.business.RateService;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/proxyMerchant")
public class MyMerchantInfoController {
    @Autowired
    UserService userService;
    @Autowired
    MerchantInfoService merchantInfoService;

    @Autowired
    PayCodeService payCodeService;

    @Autowired
    private RateService rateService;
    /**
     * 代理下的商户信息列表
     * @param merchantQueryReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListMerchantInfo", method = RequestMethod.GET)
    public String findListMerchantInfo(@ModelAttribute MerchantQueryReq merchantQueryReq, Model model, @ModelAttribute Pager pager){

        Page<MerchantInfoVo> listByCondition = merchantInfoService.findListProxyMerchantInfo(merchantQueryReq, TokenUtil.getLoginId(), pager);

        model.addAttribute("listByCondition", listByCondition).addAttribute("merchantQueryReq", merchantQueryReq)
                .addAttribute("pager", pager);
        return "business/proxy/merchant/list";

    }

    @GetMapping("/findMerchentRate")
    public String findMerchentRate(Model model,@RequestParam long userId){
        User user = userService.getRepository().findUserById(userId);
        Assert.notNull(user,"查询不到该商户");
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(userId);//查询商户信息
        Assert.notNull(user,"查询不到该商户信息");

        AddMechantInfoVo merchantInfoVo=new AddMechantInfoVo();
        merchantInfoVo.setCompanyName(merchantInfo.getCompanyName());
        merchantInfoVo.setMerchantAccount(user.getAccount());
        merchantInfoVo.setUserId(user.getId());

        List<Rate> rateList=rateService.getRepository().findByMerchantInfoId(merchantInfo.getId());

        List<PayCode> allPayCode = payCodeService.getRepository().findAll();
        List<PayCode2PayChannelBean> channelBeanList=allPayCode.stream().map(item->{
            PayCode2PayChannelBean bean=new PayCode2PayChannelBean();
            bean.setPayCodeId(item.getId());
            bean.setPayCodeName(item.getName());

            List<PayChannel> payChannelList = payCodeService.findPayChannelByPayCodeId(item.getId());

            List<RateVo> rateVoList=payChannelList.stream().map(channel->{
                RateVo vo=new RateVo();
                vo.setPayChannelId(channel.getId());
                vo.setPayChannelName(channel.getChannelName());
                vo.setChecked(false);
                for (Rate rate : rateList) {
                    if(rate.getMerchantInfoId().longValue()==merchantInfo.getId().longValue()&&channel.getId().longValue()==rate.getPayChannelId().longValue()){
                        vo.setChecked(true);
                        vo.setMerchantRate(rate.getMerchantRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setProxyRate(rate.getProxyRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setMerchantInfoId(rate.getMerchantInfoId());
                        vo.setId(rate.getId());
                    }
                }
                return vo;
            }).collect(Collectors.toList());
            bean.setRateVoList(rateVoList);
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("bean",merchantInfoVo);
        model.addAttribute("channelBeanList",channelBeanList);
        model.addAttribute("proxyList", userService.findAllProxyList());
        model.addAttribute("_method","PUT");
        return "business/proxy/merchant/form";
    }

}

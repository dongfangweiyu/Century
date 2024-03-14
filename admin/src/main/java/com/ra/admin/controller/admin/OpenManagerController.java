package com.ra.admin.controller.admin;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.req.OpenAccountReq;
import com.ra.service.bean.resp.MerchantInfoVo;
import com.ra.service.bean.resp.OpenAccountVo;
import com.ra.service.bean.resp.OpenApplyInfoVo;
import com.ra.service.bean.resp.RateVo;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.RateService;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/openManager")
public class OpenManagerController {
   @Autowired
    MerchantInfoService merchantInfoService;
   @Autowired
    UserService userService;
    @Autowired
    RateService rateService;
    @Autowired
    PayChannelService payChannelService;

    /**
     * 开户管理列表
     * @param openAccountReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListOpenManager", method = RequestMethod.GET)
    public String findListOpenManager(@ModelAttribute OpenAccountReq openAccountReq, Model model, @ModelAttribute Pager pager){

        Page<OpenAccountVo> listByCondition = merchantInfoService.findOpenAccount(openAccountReq, pager);

        model.addAttribute("listByCondition", listByCondition).addAttribute("openAccountReq", openAccountReq)
                .addAttribute("pager", pager);
        return "business/admin/open/list";

    }

    /**
     * 查看
     * @param model
     * @param id
     * @return
     */
    @GetMapping("/seeInfo")
    public String seeInfo(Model model,@RequestParam long id){
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该商户");
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(id);//查询商户信息

        User proxyUser=userService.getRepository().findUserById(merchantInfo.getProxyUserId());
        List<Rate> ratePayChannelId = rateService.getRepository().findByMerchantInfoId(merchantInfo.getId());//商户的费率信息

//        List<PayChannel> all = payChannelService.listPayChannelUserId(id);//商户的通道费率信息
//        List<OpenApplyInfoVo> rateList=all.stream().map(item->{
//            OpenApplyInfoVo rate=new OpenApplyInfoVo();
//            rate.setPayChannelId(item.getId());
//            rate.setPayChannelName(item.getChannelName());
//            for (Rate rateInfo:ratePayChannelId){
//                    for (Rate proxyRateInfo:proxyRatePayChannelId){
//                        if(item.getId().longValue()==rateInfo.getPayChannelId().longValue()&&item.getId().longValue()==proxyRateInfo.getPayChannelId().longValue()
//                                &&null!=rateInfo.getRate()&&null!=proxyRateInfo.getRate()){
//                            rate.setMerchantRate(rateInfo.getRate().multiply(BigDecimal.valueOf(100)));
//                            rate.setProxyRate(proxyRateInfo.getRate().multiply(BigDecimal.valueOf(100)));
//                            rate.setCheckedBox(true);
//                    }
//                }
//            }
//            return rate;
//        }).collect(Collectors.toList());
//        model.addAttribute("rateList", rateList);
        model.addAttribute("bean", user);
        model.addAttribute("merchantInfo", merchantInfo);
        model.addAttribute("proxyUser",proxyUser);
        model.addAttribute("_method","PUT");
        return "business/admin/open/form";
    }


    /**
     * 审核通过
     *
     * @return
     */
    @RequestMapping(value = "/applyPass", method = RequestMethod.GET)
    public String applyPass(@RequestParam("id") long id) {
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该商户信息");
        user.setStatus(0);
        userService.getRepository().save(user);
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(id);
        merchantInfo.setApplyStatus(ApplyMerchantEnum.PASS);
        merchantInfoService.getRepository().save(merchantInfo);
        return "redirect:/private/openManager/findListOpenManager";
    }

    /**
     * 审核通过
     *
     * @return
     */
    @RequestMapping(value = "/applyNotPass", method = RequestMethod.GET)
    public String applyNotPass(@RequestParam("id") long id) {
        /*User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该商户信息");
        user.setStatus(1);
        userService.getRepository().save(user);*/
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(id);
        merchantInfo.setApplyStatus(ApplyMerchantEnum.NOTPASS);
        merchantInfoService.getRepository().save(merchantInfo);
        return "redirect:/private/openManager/findListOpenManager";
    }
}

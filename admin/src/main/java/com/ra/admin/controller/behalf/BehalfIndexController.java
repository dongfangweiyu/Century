package com.ra.admin.controller.behalf;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.IpUtil;
import com.ra.dao.entity.business.BehalfInfo;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.Wallet;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.business.BehalfInfoService;
import com.ra.service.business.IncomeLogService;
import com.ra.service.business.WalletService;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/private/behalfIndex")
public class BehalfIndexController {
    @Autowired
    UserService userService;
    @Autowired
    WalletService walletService;
    @Autowired
    BehalfInfoService behalfInfoService;
    @Autowired
    IncomeLogService incomeLogService;

    /**
     * 代付商首页（卡商）
     * @param model
     * @return
     */
    @RequestMapping(value = "/findBehalfInfoIndex", method = RequestMethod.GET)
    public String findBehalfInfoIndex(Model model){
        long behalfUserId= TokenUtil.getLoginId();
        Wallet wallet=walletService.findWallet(behalfUserId);
        BehalfInfo behalfInfo=behalfInfoService.findByUserId(behalfUserId);
        behalfInfo.setBehalfRate(behalfInfo.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
        ProxyIncomeLogReq proxyIncomeLogReq=new ProxyIncomeLogReq();
        proxyIncomeLogReq.setIncomeUserId(behalfUserId);
        BigDecimal incomeLogTotal=incomeLogService.findByIncomeLogTotal(proxyIncomeLogReq);
        model.addAttribute("behalfInfo", behalfInfo).addAttribute("wallet", wallet).addAttribute("incomeLogTotal",incomeLogTotal);
        return "business/behalf/index/list";

    }

    /**
     * 绑定下单IP
     * @param model
     * @return
     */
    @GetMapping("/editBehalfIp")
    public String editBehalfIp(Model model){
        try{
            BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(TokenUtil.getLoginId());
            Assert.notNull(behalfInfo,"卡商信息不存在，无法绑定");
            model.addAttribute("behalfInfo", behalfInfo);
            return "business/behalf/behalfIp/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/behalfIndex/findBehalfInfoIndex";
    }
    /**
     * 绑定下单IP
     * @return
     */
    @RequestMapping(value = "/editBehalfIp", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult editBehalfIp(@RequestParam String behalfIp){
        ApiResult apiResult=new ApiResult();

        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(TokenUtil.getLoginId());
        Assert.notNull(behalfInfo,"卡商信息不存在，无法绑定");

        if(StringUtils.hasText(behalfIp)){
            if(IpUtil.isIp(behalfIp)){
                behalfInfo.setBehalfIp(behalfIp);
            }else{
                return apiResult.fail("IP格式不正确");
            }
        }else{
            behalfInfo.setBehalfIp(null);
        }
        behalfInfoService.getRepository().save(behalfInfo);
        return apiResult.ok();

    }
}

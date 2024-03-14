package com.ra.admin.controller.merchant;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.bean.resp.ProxyIncomeLogVo;
import com.ra.service.business.IncomeLogService;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayCodeService;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/private/merchantIncomeLog")
public class MerchantIncomeLogController {
    @Autowired
    IncomeLogService incomeLogService;

    @Autowired
    PayCodeService payCodeService;

    @Autowired
    PayChannelService payChannelService;

    @Autowired
    UserService userService;

    @RequestMapping(value = "/findListMerchantIncomeLog", method = RequestMethod.GET)
    public String findListMerchantIncomeLog(@ModelAttribute ProxyIncomeLogReq proxyIncomeLogReq, Model model, @ModelAttribute Pager pager){
//        pager.setSize(20);
        long merchantUserId= TokenUtil.getLoginId();
        proxyIncomeLogReq.setIncomeUserId(merchantUserId);
        Page<ProxyIncomeLogVo> listByCondition = incomeLogService.findListIncomeLog(proxyIncomeLogReq, pager);

        List<PayCode> paycodeList=payCodeService.getRepository().findAll();
        if(listByCondition!=null){
            for (ProxyIncomeLogVo proxyIncomeLogVo : listByCondition) {
                for (PayCode payCode : paycodeList) {
                    if(payCode.getCode().equals(proxyIncomeLogVo.getPayCode())){
                        proxyIncomeLogVo.setPayCode(payCode.getName());
                        break;
                    }
                }
            }
        }
        BigDecimal merchantIncomeLogTolal=incomeLogService.findByIncomeLogTotal(proxyIncomeLogReq);
        model.addAttribute("listByCondition", listByCondition).addAttribute("paycodeList", paycodeList)
                .addAttribute("merchantIncomeLogTolal", merchantIncomeLogTolal)
             .addAttribute("proxyIncomeLogReq", proxyIncomeLogReq)
               .addAttribute("pager", pager);
        return "business/merchant/incomeLog/list";
    }
}

package com.ra.admin.controller.behalf;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.bean.resp.ProxyIncomeLogVo;
import com.ra.service.business.IncomeLogService;
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
@RequestMapping("/private/behalfIncomeLog")
public class BehalfIncomeLogController {
    @Autowired
    IncomeLogService incomeLogService;
    @Autowired
    UserService userService;

    @RequestMapping(value = "/findListIncomeLog", method = RequestMethod.GET)
    public String findListMerchantIncomeLog(@ModelAttribute ProxyIncomeLogReq proxyIncomeLogReq, Model model, @ModelAttribute Pager pager){
//        pager.setSize(20);
        long behalfUserId= TokenUtil.getLoginId();
        proxyIncomeLogReq.setIncomeUserId(behalfUserId);
        Page<ProxyIncomeLogVo> listByCondition = incomeLogService.findListIncomeLog(proxyIncomeLogReq, pager);
        BigDecimal incomeLogTotal=incomeLogService.findByIncomeLogTotal(proxyIncomeLogReq);
        model.addAttribute("listByCondition", listByCondition)
                .addAttribute("incomeLogTotal", incomeLogTotal)
                .addAttribute("proxyIncomeLogReq", proxyIncomeLogReq)
                .addAttribute("pager", pager);
        return "business/behalf/incomeLog/list";
    }
}

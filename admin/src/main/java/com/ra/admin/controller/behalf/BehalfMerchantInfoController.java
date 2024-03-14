package com.ra.admin.controller.behalf;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.resp.*;
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
@RequestMapping("/private/behalfMerchant")
public class BehalfMerchantInfoController {
    @Autowired
    UserService userService;
    @Autowired
    MerchantInfoService merchantInfoService;

    /**
     * 卡商绑定的商户余额信息列表
     * @param merchantQueryReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListBehalfMerchantInfo", method = RequestMethod.GET)
    public String findListBehalfMerchantInfo(@ModelAttribute MerchantQueryReq merchantQueryReq, Model model, @ModelAttribute Pager pager){
        long userId=TokenUtil.getLoginId();
        Page<MerchantInfoVo> listByCondition = merchantInfoService.findListBehalfMerchantInfo(merchantQueryReq, userId, pager);
        MerchantInfoTotalVo merchantTotal = merchantInfoService.findBehalfMerchantTotal(merchantQueryReq,userId);
        model.addAttribute("listByCondition", listByCondition).addAttribute("merchantQueryReq", merchantQueryReq)
                .addAttribute("pager", pager) .addAttribute("merchantTotal", merchantTotal);
        return "business/behalf/merchantInfo/list";

    }

}

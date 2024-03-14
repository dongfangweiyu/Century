package com.ra.admin.controller.admin;


import com.ra.common.domain.Pager;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.service.bean.req.ChannelWalletLogReq;
import com.ra.service.bean.resp.ChannelWalletLogTimeVo;
import com.ra.service.business.ChannelWalletLogService;
import com.ra.service.business.ConfigPayInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * 根据时间点查某个瞬时通道的额度
 */
@Controller
@RequestMapping("/private/channelwalltLogTime")
public class ChannelWalletLogTimeController {
    @Autowired
    private ConfigPayInterfaceService configPayInterfaceService;
    @Autowired
    ChannelWalletLogService channelWalletLogService;

    /**
     * 通道瞬时金额列表
     * @param channelWalletLogReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String findListChannelWalletLogTime(@ModelAttribute ChannelWalletLogReq channelWalletLogReq, Model model, @ModelAttribute Pager pager){
        pager.setSize(20);

        List<ConfigPayInterface> configPayInterfaceList = configPayInterfaceService.getRepository().findAll();

        Page<ChannelWalletLogTimeVo> listByCondition = channelWalletLogService.findChannelWalletLogTimeList(channelWalletLogReq, pager);
        BigDecimal total=channelWalletLogService.findChannelWalletLogTimeTotal(channelWalletLogReq);
        model.addAttribute("listByCondition", listByCondition).
                addAttribute("total",total).addAttribute("channelWalletLogReq", channelWalletLogReq)
                .addAttribute("pager", pager).addAttribute("configPayInterfaceList",configPayInterfaceList);
        return "business/admin/walletLog/channelWalltLoglistTime";
    }
}

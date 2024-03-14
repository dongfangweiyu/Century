package com.ra.admin.controller.admin;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.Rate;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.AgencyQueryReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.RateService;
import com.ra.service.business.WalletService;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/proxy")
public class ProxyManagerController {

    @Autowired
    UserService userService;

    @Autowired
    MerchantInfoService merchantInfoService;

    @Autowired
    private PayChannelService payChannelService;

    @Autowired
    private RateService rateService;
    @Autowired
    WalletService walletService;

    /**
     * 代理商列表
     * @param agencyQueryReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListAgency", method = RequestMethod.GET)
    public String findListAgency(@ModelAttribute AgencyQueryReq agencyQueryReq, Model model, @ModelAttribute Pager pager){


        Page<ProxyInfoVo> listByCondition = userService.findListProxyInfo(agencyQueryReq, pager);
        MerchantInfoTotalVo proxyInfoTotal = userService.findProxyInfoTotal(agencyQueryReq);
        model.addAttribute("listByCondition", listByCondition).addAttribute("agencyQueryReq", agencyQueryReq)
                .addAttribute("proxyInfoTotal",proxyInfoTotal).addAttribute("pager", pager);
        return "business/admin/proxy/list";

    }


    @GetMapping("/add")
    public String add(Model model){
        model.addAttribute("bean",new User());
        model.addAttribute("_method","POST");
        return "business/admin/proxy/form";
    }
    /**
     * 添加代理
     * @return
     */
    @PostMapping(value = "/add")
    public String addProxy(Model model,@ModelAttribute ProxyAddInfoVo proxyAddInfoVo){
        try{
            Assert.notNull(proxyAddInfoVo.getAccount(), "账号不能为空");
            boolean i=userService.addAgencyUser(proxyAddInfoVo);
            if(i){
                return "redirect:/private/proxy/findListAgency";
            }
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",new User());
            model.addAttribute("_method","POST");
            return "business/admin/proxy/form";
        }
        return "business/admin/proxy/form";
    }

    /**
     * 编辑
     * @param model
     * @param id
     * @return
     */
    @GetMapping("/edit")
    public String edit(Model model,@RequestParam long id){
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该代理");

        model.addAttribute("bean",user);
        model.addAttribute("_method","PUT");
        return "business/admin/proxy/form";
    }

    @PutMapping("/edit")
    public String edit(Model model, @ModelAttribute ProxyAddInfoVo proxyAddInfoVo){
        User user=new User();
        try{
            user=userService.getRepository().findUserById(proxyAddInfoVo.getId());
            Assert.notNull(user,"用户不存在");
            user.setAccount(proxyAddInfoVo.getAccount()+"@proxy.com");
            userService.getRepository().save(user);
            return "redirect:/private/proxy/findListAgency";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",user);
            model.addAttribute("_method","PUT");
        }
        return "business/admin/proxy/form";
    }


    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该代理信息");

        if(user.getStatus()==0){
            user.setStatus(1);
        }else {
            user.setStatus(0);
        }
        userService.getRepository().save(user);
        return "redirect:/private/proxy/findListAgency";
    }

    /**
     * 代理详情
     * @param userId
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "/findEditProxy", method = RequestMethod.GET)
    public String findEditProxy(@RequestParam("userId") Long userId, Model model, HttpServletRequest request){
        ProxyDetailVo proxyDetailVo=new ProxyDetailVo();
        /*查询用户信息*/
        User user = userService.getRepository().findUserById(userId);
        /*查询用户账户信息*/
        Wallet wallet=walletService.findWallet(userId);

        proxyDetailVo.setProxyUserId(userId);//代理ID
        proxyDetailVo.setProxyAccount(user.getAccount());//代理账号
        proxyDetailVo.setCreateTime(user.getCreateTime());//注册时间
        proxyDetailVo.setStatus(user.getStatus());//商户状态

        proxyDetailVo.setWalletMoney(wallet.getMoney());//账户总金额
        model.addAttribute("proxyDetailVo",proxyDetailVo).addAttribute("_method", "POST");
        return "business/admin/proxy/detail";
    }

    /**
     * 弹出修改金额
     * @param model
     * @return
     */
    @GetMapping("/updateMoney")
    public String updateMoney(Model model,@RequestParam("userId") Long userId){
        try{
            model.addAttribute("userId",userId);
            return "business/admin/proxy/updateMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/proxy/findListAgency";
    }
}

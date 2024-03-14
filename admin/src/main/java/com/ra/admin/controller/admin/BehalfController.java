package com.ra.admin.controller.admin;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.BehalfQueryReq;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.*;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/behalf")
public class BehalfController {
    @Autowired
    UserService userService;

    @Autowired
    WalletService walletService;

    @Autowired
    BehalfInfoService behalfInfoService;

    /**
     * 卡商列表
     * @param behalfQueryReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListBehalf", method = RequestMethod.GET)
    public String findListBehalf(@ModelAttribute BehalfQueryReq behalfQueryReq, Model model, @ModelAttribute Pager pager){

        Page<BehalfInfoVo> listByCondition = behalfInfoService.findListBehalfInfo(behalfQueryReq, pager);
        BehalfInfoTotalVo behalfTotal = behalfInfoService.findBehalfTotal(behalfQueryReq);
        model.addAttribute("listByCondition", listByCondition).addAttribute("behalfQueryReq", behalfQueryReq)
                .addAttribute("behalfTotal",behalfTotal).addAttribute("pager", pager);
        return "business/admin/behalf/list";

    }

    @GetMapping("/add")
    public String add(Model model){
        model.addAttribute("bean",new AddBehalfInfoVo());
        model.addAttribute("_method","POST");
        return "business/admin/behalf/form";
    }

    /**
     * 添加商户
     * @return
     */
    @PostMapping(value = "/add")
    public String addMerchant(Model model,@ModelAttribute AddBehalfInfoVo addBehalfInfoVo){
        try{
            Assert.notNull(addBehalfInfoVo.getBehalfAccount(), "账号不能为空");
            Assert.notNull(addBehalfInfoVo.getCompanyName(), "卡商名称不能为空");
            Assert.notNull(addBehalfInfoVo.getBehalfRate(), "下发费率不能为空");
            Assert.notNull(addBehalfInfoVo.getBehalfFee(), "单笔费用不能为空");
            boolean i=userService.addBehalfUser(addBehalfInfoVo);
            if(i){
                return "redirect:/private/behalf/findListBehalf";
            }
            throw new IllegalArgumentException("添加失败");
        }catch (Exception e){
            add(model);
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",addBehalfInfoVo);
            model.addAttribute("_method","POST");
            return "business/admin/behalf/form";
        }
    }

    /**
     * 编辑
     * @param model
     * @param userId
     * @return
     */
    @GetMapping("/edit")
    public String edit(Model model,@RequestParam long userId){
        User user = userService.getRepository().findUserById(userId);
        Assert.notNull(user,"查询不到该卡商账户");
        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(userId);//查询卡商信息
        Assert.notNull(behalfInfo,"查询不到该卡商信息");

        AddBehalfInfoVo addBehalfInfoVo=new AddBehalfInfoVo();
        addBehalfInfoVo.setCompanyName(behalfInfo.getCompanyName());
        addBehalfInfoVo.setBehalfAccount(user.getAccount());
        addBehalfInfoVo.setUserId(user.getId());
        addBehalfInfoVo.setBehalfRate(behalfInfo.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
        addBehalfInfoVo.setBehalfFee(behalfInfo.getBehalfFee());
        model.addAttribute("bean",addBehalfInfoVo);
        model.addAttribute("_method","PUT");
        return "business/admin/behalf/form";
    }

    @PutMapping("/edit")
    public String edit(Model model, @ModelAttribute AddBehalfInfoVo addBehalfInfoVo){
        try{
            Assert.notNull(addBehalfInfoVo.getCompanyName(), "卡商名称不能为空");
            Assert.notNull(addBehalfInfoVo.getBehalfRate(), "下发费率不能为空");
            Assert.notNull(addBehalfInfoVo.getBehalfFee(), "单笔费用不能为空");
            boolean i=userService.editBehalfUser(addBehalfInfoVo);
            if(i){
                return "redirect:/private/behalf/findListBehalf";
            }
            throw new IllegalArgumentException("修改失败");
        }catch (Exception e){
            edit(model,addBehalfInfoVo.getUserId());
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",addBehalfInfoVo);
            model.addAttribute("_method","PUT");
            return "business/admin/behalf/form";
        }
    }

    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该商户信息");

        if(user.getStatus()==0){
            user.setStatus(1);
        }else {
            user.setStatus(0);
        }
        userService.getRepository().save(user);
        return "redirect:/private/behalf/findListBehalf";
    }

    /**
     * 卡商详情
     * @param userId
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "/findEditBehalf", method = RequestMethod.GET)
    public String findEditBehalf(@RequestParam("userId") Long userId, Model model, HttpServletRequest request){
        BehalfDetailVo behalfDetailVo=new BehalfDetailVo();
        /*查询用户信息*/
        User user = userService.getRepository().findUserById(userId);
        /*查询用户账户信息*/
        Wallet wallet=walletService.findWallet(userId);

        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(userId);
        behalfDetailVo.setBehalfUserId(userId);//卡商ID
        behalfDetailVo.setBehalfAccount(user.getAccount());//卡商账号
        behalfDetailVo.setCompanyName(behalfInfo.getCompanyName());//商户名称
        behalfDetailVo.setCreateTime(behalfInfo.getCreateTime());//注册时间
        behalfDetailVo.setBehalfRate(behalfInfo.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));//卡商收取下发费率
        behalfDetailVo.setBehalfFee(behalfInfo.getBehalfFee());//卡商收取单笔固额
        behalfDetailVo.setAppId(behalfInfo.getAppId());
        behalfDetailVo.setSecret(behalfInfo.getSecret());
        behalfDetailVo.setStatus(user.getStatus());//商户状态


        behalfDetailVo.setWalletMoney(wallet.getMoney());//账户总金额
        model.addAttribute("behalfDetailVo",behalfDetailVo).addAttribute("_method", "POST");
        return "business/admin/behalf/detail";
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
            return "business/admin/behalf/updateMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/behalf/findListBehalf";
    }

    /**
     * 弹出修改利润金额
     * @param model
     * @return
     */
    @GetMapping("/modifyProfitMoney")
    public String modifyProfitMoney(Model model,@RequestParam("userId") Long userId){
        try{
            model.addAttribute("userId",userId);
            return "business/admin/behalf/updateProfitMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/behalf/findListBehalf";
    }
    /**
     * 扣除利润
     * @param userId 卡商userId
     * @param  profitMoney 修改卡商利润金额
     * @return
     */
    @PostMapping("/modifyProfitMoney")
    @ResponseBody
    public ApiResult modifyProfitMoney(@RequestParam("userId") Long userId,
                                       @RequestParam("profitMoney") BigDecimal profitMoney,
                                       @RequestParam(value = "remark",required = false,defaultValue = "")String remark){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(userId, "卡商userId不能为空");
            Assert.notNull(profitMoney, "扣除的利润不能为空");
            Assert.hasText(remark,"备注不能为空");
            if(profitMoney.compareTo(BigDecimal.ZERO)<=0){
                return result.fail("金额必须大于0");
            }
            User loginInfo = TokenUtil.getLoginInfo();
            WalletLogEnum walletLogEnum= WalletLogEnum.ADMIN;
            walletLogEnum.setDescription(remark+"（管理员："+loginInfo.getAccount()+"扣除）");

            boolean i = behalfInfoService.subProfit(profitMoney, userId,walletLogEnum);
            if(i){
                return result.ok();
            }
            return result.fail("修改失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }
}

package com.ra.admin.controller.proxy;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.BankList;
import com.ra.service.business.BankCardService;
import com.ra.service.business.BankListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/private/proxyBankCard")
public class BankCardController {
    @Autowired
    BankListService bankListService;

    @Autowired
    BankCardService bankCardService;

    @GetMapping("/bankCardList")
    public String bankCardList(Model model){

        List<BankCard> all = bankCardService.findBankCardList(TokenUtil.getLoginId());
        model.addAttribute("list",all);
        return "business/proxy/bankCard/list";
    }

    @GetMapping("/add")
    public String add(Model model){
        List<BankList>listCard= bankListService.getRepository().findAll();
        model.addAttribute("bean",new BankCard());
        model.addAttribute("listCard",listCard);
        model.addAttribute("_method","POST");
        return "business/proxy/bankCard/form";
    }

    @PostMapping("/add")
    public String add(Model model, @ModelAttribute BankCard bankCard){
        try{
            Assert.hasText(bankCard.getRealName(),"户名不能为空");
            Assert.hasText(bankCard.getBankNo(),"收款卡号不能为空");
            Assert.hasText(bankCard.getBankName(),"银行卡不能为空");
            Assert.hasText(bankCard.getBankName(),"支行信息不能为空");
            bankCard.setUserId(TokenUtil.getLoginId());
            bankCardService.getRepository().save(bankCard);
            return "redirect:/private/proxyBankCard/bankCardList";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",bankCard);
            model.addAttribute("_method","POST");
        }
        return "business/proxy/bankCard/form";
    }

    @PostMapping("/del")
    @ResponseBody
    public ApiResult del(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            BankCard bankCard = bankCardService.getRepository().findById(id);
            Assert.notNull(bankCard,"查询不到该收款信息");

            bankCardService.getRepository().delete(bankCard);
            return result.ok("删除成功");
        }catch (Exception e){

            return result.fail(e.getMessage());
        }
    }
}

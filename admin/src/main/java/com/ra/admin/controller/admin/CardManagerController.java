package com.ra.admin.controller.admin;

import com.ra.common.base.ApiResult;
import com.ra.dao.entity.business.BankList;
import com.ra.service.business.BankListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/private/card")
public class CardManagerController {
    @Autowired
    BankListService bankListService;

    @GetMapping("/list")
    public String list(Model model){

        List<BankList> all = bankListService.getRepository().findAll();
        model.addAttribute("list",all);
        return "business/admin/card/list";
    }


    @GetMapping("/add")
    public String add(Model model){
        model.addAttribute("bean",new BankList());
        model.addAttribute("_method","POST");
        return "business/admin/card/form";
    }

    @PostMapping("/add")
    public String add(Model model, @ModelAttribute BankList bankList){
        try{
            Assert.hasText(bankList.getBankName(),"银行卡名称不能为空");
            bankListService.getRepository().save(bankList);
            return "redirect:/private/card/list";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean", bankList);
            model.addAttribute("_method","POST");
        }
        return "business/admin/card/form";
    }

    @PostMapping("/del")
    @ResponseBody
    public ApiResult del(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            BankList bankList = bankListService.getRepository().findById(id);
            Assert.notNull(bankList,"查询不到该银行卡");

            bankListService.getRepository().delete(bankList);
            return result.ok("删除成功");
        }catch (Exception e){

            return result.fail(e.getMessage());
        }
    }
}

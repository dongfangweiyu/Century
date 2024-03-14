package com.ra.admin.controller.admin;

import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.security.User;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.business.PayCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 支付编码管理
 */
@Controller
@RequestMapping("/private/payCode")
public class PayCodeController {

    @Autowired
    private PayCodeService payCodeService;

    @GetMapping("/list")
    public String list(Model model){

        List<PayCode> all = payCodeService.getRepository().findAll();
        model.addAttribute("list",all);
        return "business/admin/payCode/list";
    }

    @GetMapping("/add")
    public String add(Model model){
        model.addAttribute("bean",new PayCode());
        model.addAttribute("_method","POST");
        return "business/admin/payCode/form";
    }

    @PostMapping("/add")
    public String add(Model model, @ModelAttribute PayCode payCode){
        try{
            Assert.hasText(payCode.getName(),"名称不能为空");
            Assert.hasText(payCode.getCode(),"编码不能为空");
            payCodeService.getRepository().save(payCode);
            return "redirect:/private/payCode/list";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",payCode);
            model.addAttribute("_method","POST");
        }
        return "business/admin/payCode/form";
    }

    @GetMapping("/edit")
    public String edit(Model model,@RequestParam long id){

        PayCode payCode = payCodeService.getRepository().findById(id);
        Assert.notNull(payCode,"查询不到该支付编码");

        model.addAttribute("bean",payCode);
        model.addAttribute("_method","PUT");
        return "business/admin/payCode/form";
    }

    @PutMapping("/edit")
    public String edit(Model model, @ModelAttribute PayCode payCode){
        try{
            PayCode old = payCodeService.getRepository().findById(payCode.getId().longValue());
            Assert.notNull(payCode,"查询不到该支付编码");

            Assert.hasText(payCode.getName(),"名称不能为空");
            Assert.hasText(payCode.getCode(),"编码不能为空");

            old.setName(payCode.getName());
            old.setCode(payCode.getCode());

            payCodeService.getRepository().save(old);
            return "redirect:/private/payCode/list";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",payCode);
            model.addAttribute("_method","PUT");
        }
        return "business/admin/payCode/form";
    }


    @PostMapping("/del")
    @ResponseBody
    public ApiResult del(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            PayCode payCode = payCodeService.getRepository().findById(id);
            Assert.notNull(payCode,"查询不到该支付编码");

            payCodeService.getRepository().delete(payCode);
            return result.ok("删除成功");
        }catch (Exception e){

            return result.fail(e.getMessage());
        }
    }


    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        PayCode payCode = payCodeService.getRepository().findById(id);
        Assert.notNull(payCode,"查询不到该支付编码");

        if(payCode.isEnable()){
            payCode.setEnable(false);
        }else {
            payCode.setEnable(true);
        }
        payCodeService.getRepository().save(payCode);
        return "redirect:/private/payCode/list";
    }
}

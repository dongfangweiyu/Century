package com.ra.admin.controller.admin;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.business.*;
import com.ra.service.component.ClearTenDaysDataComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 系统设置
 */
@Controller
@RequestMapping("/private/config")
public class ConfigController {

    @Autowired
    ClearTenDaysDataComponent clearTenDaysDataComponent;
    @GetMapping("/")
    public String website(Model model){

        model.addAttribute("userId", TokenUtil.getLoginId());
        for (ConfigEnum value : ConfigEnum.values()) {
            model.addAttribute(value.toString(),ConfigFactory.get(value));
        }
        return "business/admin/website";
    }

    @PostMapping("/")
    public String postWebsite(HttpServletRequest request){
        Map<String, String[]> parameterMap = request.getParameterMap();

        for (String key : parameterMap.keySet()) {
            ConfigFactory.set(ConfigEnum.valueOf(key),parameterMap.get(key)[0]);
        }
        return "redirect:/private/config/";
    }

    @PostMapping("/clearTenDaysData")
    @ResponseBody
    public ApiResult clearTenDaysData(){
        ApiResult apiResult=new ApiResult();
        Long userId=TokenUtil.getLoginId();
        if(userId==1){
            boolean b=clearTenDaysDataComponent.clear();
            if(b){
                return apiResult.ok("清除成功");
            }else{
                return apiResult.fail("操作失败");
            }
        }else{
            return apiResult.fail("无权限操作");
        }

    }
}

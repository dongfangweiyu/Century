package com.ra.admin.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/private/")
public class LogController {

    @RequestMapping("/log")
    public String log(Model model, @RequestParam(defaultValue = "") String dir){
        model.addAttribute("dir",dir);
        return "business/admin/tail";
    }
}

package com.ra.admin.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/private/index")
public class IndexController {

    @RequestMapping("/console")
    public String console(){
        return "views/home/console";
    }
}

package com.ra.admin.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 监听404
 */
@Controller
class MainsiteErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model, Map<String, Object> params){
        //获取statusCode:401,404,500
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
         if(statusCode == 404){
            return "404";
        }else{
            return getErrorPath();
        }
    }

    @Override
    public String getErrorPath() {
        return "error";
    }
}
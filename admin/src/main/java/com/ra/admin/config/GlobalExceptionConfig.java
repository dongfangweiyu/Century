package com.ra.admin.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 * 注意这种处理器是监听不到404的
 * @author ouyang
 */
@ControllerAdvice
public class GlobalExceptionConfig {
    private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionConfig.class);

    @ExceptionHandler(value = Exception.class)
    public String errorHandler(Exception e, Model model) throws Exception {
        logger.error(e.getMessage(), e);
        model.addAttribute("message",e.getMessage());
        return "error";
    }
}

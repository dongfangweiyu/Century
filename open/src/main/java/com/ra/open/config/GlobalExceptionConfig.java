package com.ra.open.config;

import com.ra.common.base.ApiResult;
import com.ra.common.exception.ConfigIllegalException;
import com.ra.common.exception.TokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.BindException;

/**
 * 全局异常处理器
 * @author ouyang
 *
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionConfig {
	private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionConfig.class);

	 /**
     * 所有异常报错
     * @param response
     * @param e
     * @return
     * @throws Exception
     */
    @ExceptionHandler(value=Exception.class)  
    public void allExceptionHandler(HttpServletResponse response,
            Exception e) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();//声明错误流输出对象
        PrintStream pout = new PrintStream(out);//通过输出对象获取打印流
        e.printStackTrace(pout);//读取错误流到打印流
        String exceptionMessage = new String(out.toByteArray());//把打印流流转换成String

        ApiResult result=new ApiResult(-1,exceptionMessage);

        if(e instanceof NullPointerException){
            result.fail("空指针异常");
        }

        if(e instanceof IllegalArgumentException){
            result.fail(e.getMessage());
        }

        if(e instanceof IOException){
            result.fail("IO流异常");
        }

        if(e instanceof DataIntegrityViolationException){
            DataIntegrityViolationException dataIntegrityViolationException= (DataIntegrityViolationException)e;
            result.fail("参数不合法:"+dataIntegrityViolationException.getCause().getCause().getMessage());
        }
        if(e instanceof ConfigIllegalException){
            result=((ConfigIllegalException) e).getApiResult();
        }

        if(e instanceof BindException){
            result.fail("参数异常："+e.getMessage());
        }

        if(e instanceof HttpRequestMethodNotSupportedException){
            result.fail("请求方式："+e.getMessage());
        }

        if(e instanceof MissingServletRequestParameterException){
            result.fail("缺少"+((MissingServletRequestParameterException) e).getParameterName()+"参数");
        }

        //如果是token验证异常，返回-200 ，跳转到登录页
        if(e instanceof TokenException){
            result=((TokenException) e).getApiResult();
        }

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.write(result.toString());
        printWriter.flush();
        printWriter.close();
//        throw e;
        logger.error(exceptionMessage);
    }
}

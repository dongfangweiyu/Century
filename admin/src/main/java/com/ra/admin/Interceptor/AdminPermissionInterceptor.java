package com.ra.admin.Interceptor;

import com.ra.admin.utils.TokenUtil;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 后台访问权限拦截器
 *
 * @author Administrator
 */
public class AdminPermissionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminPermissionInterceptor.class);

    /**
     * 进入controller层之前拦截请求
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        httpServletRequest.setCharacterEncoding("UTF-8");
        //输出json字符串
//    	httpServletResponse.setContentType("application/json;charset=UTF-8");
        //输出html文档
        httpServletResponse.setContentType("text/html");
        httpServletResponse.setCharacterEncoding("UTF-8");

        logger.info("---当前请求路径：" + httpServletRequest.getRequestURI()+",权限检查---");

        if (handler instanceof HandlerMethod) {
            User loginInfo = TokenUtil.getLoginInfo();
            HandlerMethod h = (HandlerMethod) handler;
            String name = h.getBean().getClass().getPackage().getName();

            //ADMIN的所有请求都不进行权限拦截
            if(loginInfo.getAgencyType()== AgencyEnum.ADMIN){
                logger.info("---权限检查通过---");
                return true;
            }

            //公用的controller不进行权限拦截
            if(name.equalsIgnoreCase("com.ra.admin.controller")){
                logger.info("---权限检查通过---");
                return true;
            }

            //有权限区分的controller根据控制器所在的包名进行权限拦截
            if(("com.ra.admin.controller."+loginInfo.getAgencyType().toString()).equalsIgnoreCase(name)){
                logger.info("---权限检查通过---");
                return true;
            }

            logger.info("---权限检测未通过，重定向到首页---");
            PrintWriter printWriter = httpServletResponse.getWriter();
            printWriter.flush();
            printWriter.println("<script language='javascript'>top.location.href='/';</script>");
            printWriter.close();
            return false;
        }
        logger.info("---权限检测未通过，重定向到首页---");
        return false;
    }

    /**
     * --------------处理请求完成后视图渲染之前的处理操作---------------
     */
    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        // System.out.println("");
//        if (modelAndView != null) {
//            //记录请求视图
//            logger.info("ViewName: " + modelAndView.getViewName());
//
////            if (httpServletResponse.getStatus() == 500) {
////                modelAndView.setViewName("/errorpage/500");
////            } else if (httpServletResponse.getStatus() == 404) {
////                modelAndView.setViewName("/errorpage/404");
////            }
//
//        } else {
//            logger.info("ViewName: 404 not found...");
//        }


    }

    /**
     * ---------------视图渲染之后的操作-------------------------
     */
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {


    }
}

package com.ra.admin.Interceptor;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 后台登录拦截器
 *
 * @author Administrator
 */
public class AdminTokenInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminTokenInterceptor.class);
    private NamedThreadLocal<Long> threadLocal = new NamedThreadLocal<Long>("StartTime-EndTime");

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

        Long startTime = System.currentTimeMillis();//1、开始时间
        threadLocal.set(startTime);//线程绑定变量（该数据只有当前请求的线程可见）

        logger.info("---当前请求路径：" + httpServletRequest.getRequestURI()+",登录检查---");

        boolean loginFlag = TokenUtil.verifyLoginInfo(httpServletResponse);
        //检测登录账号的访问权限
        if (loginFlag) {
            // 输出HTTP request 信息
            // 所有请求第一个进入的方法
            String reqURL = httpServletRequest.getRequestURL().toString();
            String ip = IpUtil.getIpAddr(httpServletRequest);

            if (handler instanceof HandlerMethod) {
                StringBuilder sb = new StringBuilder(1000);
                HandlerMethod h = (HandlerMethod) handler;
                //Controller 的包名
                sb.append("Controller: ").append(h.getBean().getClass().getName()).append("\n");
                //方法名称
                sb.append("Method    : ").append(h.getMethod().getName()).append("\n");
                //请求方式  post\put\get 等等
                sb.append("RequestMethod    : ").append(httpServletRequest.getMethod()).append("\n");
                //所有的请求参数
                sb.append("Header Params    : ").append("").append("\n");
                //所有的请求参数
                sb.append("From-data Params    : ").append(httpServletRequest.getQueryString()).append("\n");
                //所有的请求参数
//                sb.append("Body Params    : ").append(IOUtils.toString(httpServletRequest.getInputStream())).append("\n");
                //部分请求链接
                sb.append("URI       : ").append(httpServletRequest.getRequestURI()).append("\n");
                //完整的请求链接
                sb.append("AllURI    : ").append(reqURL).append("\n");
                //请求方的 ip地址
                sb.append("request IP: ").append(ip);
//                logger.info("\n"+sb.toString());
            }
            logger.info("---登录检查通过---");
            return true;
        } else {
//        	if (request.getHeader("x-requested-with") != null && request.getHeader("x-requested-with").equalsIgnoreCase("XMLHttpRequest")){ 
//                //如果是ajax请求响应头会有，x-requested-with  
//                System.out.print("发生ajax请求...");
//                return true;
//                //httpServletResponse.setHeader("sessionstatus", "timeout");//在响应头设置session状态  
//            }else{
//                System.out.print("返回主页...");
//                httpServletRequest.getRequestDispatcher("/index.do").forward(request, response);//转发到登录界面 
//            }  
//        	
//            PrintWriter printWriter = httpServletResponse.getWriter();  
//            printWriter.write(new ApiResult(-200, "请先登录").toString()); 
//            printWriter.flush();
//            printWriter.close();
//            System.out.println("-------------------本次请求被成功拦截-----------------");
//        	httpServletRequest.getRequestDispatcher("/").forward(httpServletRequest, httpServletResponse);//转发到登录界面
//        	httpServletResponse.sendRedirect("/");//重定向到登录页面
            logger.info("---登录检查未通过,该请求已被拦截，并重定向到登录页面---");
            PrintWriter printWriter = httpServletResponse.getWriter();
            printWriter.flush();
            printWriter.println("<script language='javascript'>top.location.href='/';</script>");
            printWriter.close();
            return false;
        }
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
        Long startTime = threadLocal.get();//得到线程绑定的局部变量（开始时间）
        Long endTime = System.currentTimeMillis();//2、结束时间
        // new SimpleDateFormat("hh:mm:ss.SSS").format(endTime);
//        String result = "URI:" + httpServletRequest.getRequestURI() +
//                "\nController(handle):" + o +
//                "\n开始计时:" + new SimpleDateFormat("hh:mm:ss.SSS").format(startTime) +
//                "\n结束时间:" + new SimpleDateFormat("hh:mm:ss.SSS").format(endTime) +
//                "\n耗时:" + (endTime - startTime) + "ms" +
//                "\n最大内存:" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "M" +
//                "\n已分配内存:" + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "M" +
//                "\n已分配内存的剩余空间:" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "M" +
//                "\n最大可用内存:" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "M";
//
//        logger.info(result);
        logger.info("---拦截器通过本次请求"+ httpServletRequest.getRequestURI()+"，本次请求耗时：" + (endTime - startTime) + "豪秒");


    }
}

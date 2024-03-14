package com.ra.open.Interceptor;

import com.ra.common.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Api请求日志拦截器
 *
 * @author Administrator
 */
public class OpenRequestLogInterceptor implements HandlerInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(OpenRequestLogInterceptor.class.getName());
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
        Long startTime = System.currentTimeMillis();//1、开始时间
        threadLocal.set(startTime);//线程绑定变量（该数据只有当前请求的线程可见）
        logger.info("---当前请求路径：" + httpServletRequest.getRequestURI());

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
            sb.append("Header Params    : ").append("\n");
            //Query请求参数
            sb.append("Query Params    : ").append(httpServletRequest.getQueryString()).append("\n");
            //获取form Data参数
            sb.append("From-data Params    : ").append(getFormDataParameter(httpServletRequest)).append("\n");
            //部分请求链接
            sb.append("URI       : ").append(httpServletRequest.getRequestURI()).append("\n");
            //完整的请求链接
            sb.append("AllURI    : ").append(reqURL).append("\n");
            //请求方的 ip地址
            sb.append("request IP: ").append(ip);
            logger.info("\n"+sb.toString());
        }
        return true;
    }

    /**
     * --------------处理请求完成后视图渲染之前的处理操作---------------
     */
    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        // System.out.println("");
//    	if (modelAndView != null){
//            //记录请求视图
//            logger.info("ViewName: " + modelAndView.getViewName());
//        }else{
//            logger.info("ViewName: No by Api!");
//        }
    }

    /**
     * ---------------视图渲染之后的操作-------------------------
     */
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        Long startTime = threadLocal.get();//得到线程绑定的局部变量（开始时间）
        Long endTime = System.currentTimeMillis();//2、结束时间

//        String result = "URI:" + httpServletRequest.getRequestURI() +
//                "\nController(handle):" + o +
//                         "\n开始计时:" + new SimpleDateFormat("hh:mm:ss.SSS").format(startTime) +
//                         "\n结束时间:"+  new SimpleDateFormat("hh:mm:ss.SSS").format(endTime) +
//                "\n最大内存:" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "M" +
//                "\n已分配内存:" + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "M" +
//                "\n已分配内存的剩余空间:" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "M" +
//                "\n最大可用内存:" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "M" +
//                "\n耗时:" + (endTime - startTime) + "ms";

//        logger.info(result);
        logger.info("---"+httpServletRequest.getRequestURI()+"本次请求耗时：" + (endTime - startTime) + "豪秒---------------------");
    }


    /**
     * 获取请求的所有form参数
     * key-value
     * @param request
     */
    private String getFormDataParameter(HttpServletRequest request){
        StringBuffer sb=new StringBuffer();
        Enumeration enu = request.getParameterNames();
        while (enu.hasMoreElements()) {
            String paraName = (String) enu.nextElement();
            sb.append("&"+paraName+"="+request.getParameter(paraName));
        }
        return sb.toString();
    }
}

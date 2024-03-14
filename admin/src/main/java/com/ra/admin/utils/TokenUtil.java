package com.ra.admin.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.configuration.SpringContextHolder;
import com.ra.common.utils.EncryptUtil;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.security.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author huli
 * @time 2019-01-08
 * @description token 工具类
 */
public class TokenUtil {
	
	private final static Logger logger=LoggerFactory.getLogger(TokenUtil.class);
	/**
	 *
	 * 获取HttpServletRequest
	 * 
	 * @return
	 */
	private static HttpServletRequest getRequest() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes sra = (ServletRequestAttributes) ra;
		HttpServletRequest request = sra.getRequest();
		return request;
	}

    /**
     * 设置登录信息
     * @return
     */
    public static void setLoginInfo(HttpServletResponse response,User agency) {
    	//保存登录信息
    	Cookie cookie=new Cookie("sessionId", EncryptUtil.encryptAES(JSONObject.toJSONString(agency), "cypal2"));
    	cookie.setPath("/");
    	cookie.setMaxAge(24 * 60 * 60);// 设置为1天
    	response.addCookie(cookie);
    }

	/**
	 * 获取登录信息
	 * @return
	 */
	public static User  getLoginInfo() {
		Cookie[] cookies = getRequest().getCookies();
		if(cookies != null){
			for(Cookie cookie : cookies){
				if(cookie.getName().equals("sessionId")){
					String value=	EncryptUtil.decryptAES(cookie.getValue(), "cypal2");
					if(!StringUtils.isEmpty(value)){
						return JSON.parseObject(value, User.class);
					}
				}
			}
		}
		return  null;
	}

	/**
	 * 获取登录信息ID
	 * @return
	 */
	public static Long getLoginId() {
		User loginInfo = getLoginInfo();
		if(loginInfo==null){
			throw new IllegalArgumentException("请先登录");
		}
		return  loginInfo.getId();
	}
    /**
     * 移除登录信息
     */
    public static void removeLoginInfo(HttpServletResponse response){
    	Cookie[] cookies = getRequest().getCookies();
    	if(cookies != null){
            for(Cookie cookie : cookies){
                cookie.setValue("");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);  
            }
        }
    }
    /**
     * 验证登录信息
     * @return
     */
    public static boolean verifyLoginInfo(HttpServletResponse response){
		User loginInfo = getLoginInfo();
    	if(loginInfo==null){
    		logger.info("没有登录信息，跳转到登录页...");
    		return false;
    	}
    	//验证一下登录账户和密码
    	UserRepository agencyRepository = SpringContextHolder.getBean(UserRepository.class);
		User agency = agencyRepository.findUserByAccountAndPassword(loginInfo.getAccount(), loginInfo.getPassword());
        if(agency==null||agency.getStatus()==1){
        	logger.info("登录信息发生更改，踢出到登录页...");
        	removeLoginInfo(response);
        	return false;
        }
        return true;
    }
}

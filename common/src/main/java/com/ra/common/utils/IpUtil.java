package com.ra.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取IP
 */
public final class IpUtil {

    private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);

    /**
     * 获取当前服务器host
     * 例如：  http://localhost:8882/
     *
     * @param request
     * @return
     */
    public static String getHost(HttpServletRequest request) {
        String host="";
        if (request.getServerPort() == 80 || request.getServerPort() == 443) {
            host= request.getScheme() + "://" + request.getServerName() + request.getContextPath();
            logger.info("IpUtil->getHost："+host);
            return host;
        }
        host= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        logger.info("IpUtil->getHost："+host);
        return host;
    }

    /**
     * 获取静态资源地址
     *
     * @param request
     * @return
     */
    public static String getStatic(HttpServletRequest request) {
        return getHost(request) + File.separator + "static/";
    }

    /**
     * 获取IP地址
     *
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress = "";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }


    /**
     * 获取IP所属城市
     *
     * @param ipAddr
     * @return
     */
    public static String getIpCity(String ipAddr) {
        try {
            logger.info("IpUtil获取到IP：" + ipAddr);
            String result = HttpUtil.doPOST("http://ip-api.com/json/" + ipAddr + "?lang=zh-CN");
            logger.info("IpUtil向ip-api网关发出请求：http://ip-api.com/json/" + ipAddr + "?lang=zh-CN");
            logger.info("IpUtil获取到IP归属地Json：" + result);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.getString("status").equalsIgnoreCase("success")) {
                String city = jsonObject.getString("city");
                if (StringUtils.isEmpty(city)) {
                    return "未知地区";
                }
                return city;
            }
        } catch (Exception e) {
            return "未知地区";
        }
        return "未知地区";
    }


    /**
     *      * 判断一个字符串是否为url
     *      * @param str String 字符串
     *      * @return boolean 是否为url
     *      * @author peng1 chen
     *      *
     **/
    public static boolean isURL(String str) {
        str = str.toLowerCase();//转换为小写
        String regex = "^((https|http|ftp|rtsp|mms)?://)" //https、http、ftp、rtsp、mms
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@  
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184  
                + "|" // 允许IP和DOMAIN（域名）
                + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.  
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名  
                + "[a-z]{2,7})" // first level domain- .com or .museum  
                + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
                + "((/?)|" // a slash isn't required if there is no file name  
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";
        return str.matches(regex);
    }

    /**
     * 判断是否是IP
     * @param ip
     * @return
     */
    public static boolean isIp(String ip) {
        Pattern pattern = Pattern.compile("(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])){3}(\\s*,\\s*(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])){3})*"); // IP正则
        Matcher match = pattern.matcher(ip);
        return match.matches();
    }
}

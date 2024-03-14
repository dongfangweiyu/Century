package com.ra.service.channel;

import com.alibaba.fastjson.JSONObject;
import com.ra.common.component.RedisComponent;
import com.ra.common.configuration.SpringContextHolder;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.channel.PayInterface;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.dao.repository.business.PayChannelRepository;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.business.BehalfBankCardService;
import com.ra.service.business.PayOrderService;
import com.ra.service.business.WalletService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * GodsPay 接口
 * 与第三方对接的具体实现
 */
public abstract class CommonPayInterface implements PayInterface {

    protected static final Logger logger= LoggerFactory.getLogger(CommonPayInterface.class);

    protected CommonPayInterface(){
        init();
    }

    protected static RedisComponent redisComponent;
    protected static PayOrderService payOrderService;
    protected static PayChannelRepository payChannelRepository;
    protected static ConfigPayInterfaceRepository configPayInterfaceRepository;
    protected static WalletService walletService;
    protected static BehalfBankCardService behalfBankCardService;

    private void init() {
        if(redisComponent==null){
            System.out.println("init redisComponent.");
            redisComponent=SpringContextHolder.getBean(RedisComponent.class);
        }
        if(payOrderService==null){
            payOrderService=SpringContextHolder.getBean(PayOrderService.class);
        }
        if(payChannelRepository==null){
            payChannelRepository=SpringContextHolder.getBean(PayChannelRepository.class);
        }
        if(configPayInterfaceRepository==null){
            configPayInterfaceRepository=SpringContextHolder.getBean(ConfigPayInterfaceRepository.class);
        }
        if(walletService==null){
            walletService=SpringContextHolder.getBean(WalletService.class);
        }
        if(behalfBankCardService==null){
            behalfBankCardService=SpringContextHolder.getBean(BehalfBankCardService.class);
        }
    }

    /**
     * 验证回调IP
     * @param config
     * @param request
     * @return
     */
    protected boolean verifyIP(ConfigPayInterface config, HttpServletRequest request){
        String bindIp = config.getBindIP();
        //如果没有绑定IP
        if(org.springframework.util.StringUtils.isEmpty(bindIp)){
            return true;
        }
        logger.info(config.getConfigName()+"绑定的回调IP："+bindIp);
        String ipAddr = IpUtil.getIpAddr(request);
        logger.info("本次回调的IP："+ipAddr);
        if(bindIp.contains(ipAddr)){//如果有这个IP，就返回验证通过
            return true;
        }
        return false;
    }
    /**
     * 从HttpServletRequest中获取请求参数
     * 转化为Map返回
     * @param request
     * @return
     */
    protected Map<String,Object> getRequestParams(HttpServletRequest request){
        Map<String,Object> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            params.put(name, request.getParameter(name));
        }
        return params;
    }
    /**
     * 创建订单签名
     * @description 生成参数签名
     * @return 回调参数签名
     */
    protected String getSign(Map<String,Object> map, String key) {

        ArrayList<String> list = new ArrayList<String>();
        for(Map.Entry<String,Object> entry:map.entrySet()){
            if(null != entry.getValue() && !"".equals(entry.getValue())){
                if(entry.getValue() instanceof JSONObject) {
                    list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
                }else {
                    list.add(entry.getKey() + "=" + entry.getValue() + "&");
                }
            }
        }
        int size = list.size();
        String [] arrayToSort = list.toArray(new String[size]);
        Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < size; i ++) {
            sb.append(arrayToSort[i]);
        }
        String result = sb.toString();
        result += "key=" + key;
        logger.info("签名加密源串："+result);
        result = EncryptUtil.encodeMD5(result, "UTF-8").toUpperCase();
        logger.info("签名加密结果："+result);
        return result;
    }


    private String getSortJson(JSONObject obj){
        SortedMap map = new TreeMap();
        Set<String> keySet = obj.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            Object vlaue = obj.get(key);
            map.put(key, vlaue);
        }
        return JSONObject.toJSONString(map);
    }


    protected String getBody(HttpServletRequest request) {
        String requestBody = null;
        try {
            requestBody= IOUtils.toString(request.getInputStream());
            if(StringUtils.isEmpty(requestBody)){
                Enumeration<String> parameterNames = request.getParameterNames();
                if(parameterNames.hasMoreElements()){
                    requestBody=parameterNames.nextElement();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestBody;
    }
}

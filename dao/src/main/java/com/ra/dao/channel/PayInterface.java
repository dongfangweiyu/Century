package com.ra.dao.channel;

import com.ra.common.base.ApiResult;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * 通道接口的底层基类
 */
public interface PayInterface {

    ApiResult onPay(HttpServletRequest request,PayOrder payOrder, PayChannel payChannel) throws Exception;

    String onNotify(HttpServletRequest request);

    /**
     * 该方法不一定要实现
     * 主要是用于跳转到自定义付款页面的
     * 如果第三方有实现付款页面就不需要实现
     * 如果第三方无付款页面，只有付款数据，则需要实现该方法
     * @param model
     * @param payOrder
     * @param jsonData
     * @throws Exception
     */
    String  parseData(Model model,PayOrder payOrder, String jsonData) throws Exception;
}

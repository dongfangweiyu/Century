package com.ra.service.bean.params;

import com.alibaba.fastjson.JSON;
import com.ra.common.base.ApiResult;
import com.ra.common.configuration.SpringContextHolder;
import com.ra.dao.channel.PayInterface;
import com.ra.dao.channel.PayInterfaceFactory;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.repository.business.PayOrderRepository;
import com.ra.service.business.PayOrderService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * OTC创建订单返回bean
 */
@Data
public class CreateOrderResp {

    private static final Logger logger= LoggerFactory.getLogger(CreateOrderResp.class);

    private static PayOrderRepository payOrderRepository;

    public CreateOrderResp(PayOrder payOrder){
        this.payOrder=payOrder;
        this.payChannel= JSON.parseObject(payOrder.getPayChannelInfoJson(),PayChannel.class);
        if(payOrderRepository==null){
            payOrderRepository= SpringContextHolder.getBean(PayOrderRepository.class);
        }
    }

    private PayOrder payOrder;

    private PayChannel payChannel;

    public ApiResult onPay(HttpServletRequest request) {
        ApiResult result=new ApiResult();
        try{
            PayInterface payInterface =PayInterfaceFactory.newInstance(payChannel.getPayInterface());
            result= payInterface.onPay(request,payOrder,payChannel);
            if(result.getCode()!=1){
                payOrderRepository.updateErrorMsg(payOrder.getId(),result.getMsg());
            }
            return result;
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            payOrderRepository.updateErrorMsg(payOrder.getId(),e.getMessage());
            return result.fail(e.getMessage());
        }
    }
}

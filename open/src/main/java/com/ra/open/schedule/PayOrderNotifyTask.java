package com.ra.open.schedule;

import com.ra.dao.entity.business.BehalfOrder;
import com.ra.service.business.BehalfOrderService;
import com.ra.service.business.PayOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class PayOrderNotifyTask {
    @Autowired
    private PayOrderService payOrderService;
    @Autowired
    private BehalfOrderService behalfOrderService;
    /**
     * 异步通知
     */
    @Scheduled(cron = "0/10 * * * * ?")//每三十秒检测一下有没有需要通知的订单，有就去通知
    public void notifyTask(){
        payOrderService.notifyTaskOrder();
        behalfOrderService.notifyTaskOrder();
    }
}

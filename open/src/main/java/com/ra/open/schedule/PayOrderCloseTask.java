package com.ra.open.schedule;

import com.ra.service.business.PayOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class PayOrderCloseTask {
    @Autowired
    private PayOrderService payOrderService;
    /**
     * 关闭订单
     */
    @Scheduled(cron = "0 0/1 * * * ?")//每分钟检测一下有没有需要关闭的订单，有就去关闭
    public void notifyTask(){
        payOrderService.closeTaskOrder();
    }
}

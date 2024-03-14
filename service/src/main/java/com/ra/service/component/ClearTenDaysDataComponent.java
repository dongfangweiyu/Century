package com.ra.service.component;

import com.ra.service.business.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * 清除数据
 */
@Component
public class ClearTenDaysDataComponent {

    private static final Logger logger= LoggerFactory.getLogger(ClearTenDaysDataComponent.class);

    @Autowired
    BehalfOrderService behalfOrderService;

    public boolean clear(){
            try{
                String sql="delete from business_behalf_order WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                int count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_behalf_bankcard_wallet_log WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_behalf_profit_log WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_channel_wallet_log WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY) ";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_income_log WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_order_pay WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_wallet_log WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());

                sql="delete from business_order_withdraw WHERE createTime < DATE_SUB(CURDATE(), INTERVAL 10 DAY)";
                count = behalfOrderService.execute(sql, new HashMap<>());
                   return  true;
        }catch (Exception e){
            return false;
        }

    }
}

package com.ra.service.business.impl;

import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.repository.business.WalletRepository;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.business.ChannelWalletLogService;
import com.ra.service.business.ConfigPayInterfaceService;
import com.ra.service.business.WalletLogService;
import com.ra.service.business.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @author ouyang
 * @time 2019-01-24
 * @description
 */
@Service
public class ConfigPayInterfaceServiceImpl extends BaseServiceImpl<ConfigPayInterfaceRepository> implements ConfigPayInterfaceService {

    @Autowired
    private ChannelWalletLogService channelWalletLogService;

    @Override
    public int addMoney(long configPayInterfaceId, BigDecimal amount, String description) {
        boolean b = channelWalletLogService.saveWalletLog(configPayInterfaceId, amount, description);
        if(b){
            return getRepository().addMoney(amount, configPayInterfaceId);
        }
        throw new IllegalArgumentException("增加通道金额失败");
    }

    @Override
    public int subMoney(long configPayInterfaceId, BigDecimal amount, String description) {
        boolean b= channelWalletLogService.saveWalletLog(configPayInterfaceId,BigDecimal.ZERO.subtract(amount),description);
        if(b){
            return getRepository().subMoney(amount,configPayInterfaceId);
        }
        throw new IllegalArgumentException("减少通道金额失败");
    }
}

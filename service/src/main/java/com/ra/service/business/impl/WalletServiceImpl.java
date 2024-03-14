package com.ra.service.business.impl;

import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.repository.business.WalletRepository;
import com.ra.service.base.impl.BaseServiceImpl;
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
public class WalletServiceImpl extends BaseServiceImpl<WalletRepository> implements WalletService {

    @Autowired
    private WalletLogService walletLogService;

    @Override
    @Transactional
    public Wallet findWallet(long userId){
        Wallet walletByUserId = repository.findWalletByUserId(userId);
        if(walletByUserId==null){
            walletByUserId=new Wallet();
            walletByUserId.setUserId(userId);
            walletByUserId.setMoney(BigDecimal.ZERO);
            walletByUserId=repository.save(walletByUserId);
        }
        return walletByUserId;
    }
    

    @Override
    public int addMoney(long userId, BigDecimal amount, WalletLogEnum logEnum) {
        boolean b = walletLogService.saveWalletLog(userId, amount, logEnum);
        if(b){
            int i = getRepository().addMoney(amount, userId);
            if(i>0){
                return i;
            }
        }
        throw new IllegalArgumentException("增加金额失败");
    }

    @Override
    public int subMoney(long userId, BigDecimal amount, WalletLogEnum logEnum) {
        boolean b= walletLogService.saveWalletLog(userId,BigDecimal.ZERO.subtract(amount),logEnum);
        if(b){
            int i= getRepository().subMoney(amount,userId);
            if(i>0){
                return i;
            }
        }
        throw new IllegalArgumentException("减少金额失败");
    }
}

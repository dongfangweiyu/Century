package com.ra.service.business;

import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.repository.business.WalletRepository;
import com.ra.service.base.BaseService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @author huli
 * @time 2019-01-24
 * @description
 */
public interface WalletService extends BaseService<WalletRepository> {

    /**
     * 查询钱包bean信息
     * @return
     */
    Wallet findWallet(long userId);

    /**
     * 根据userId获取钱包
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int addMoney(long userId, BigDecimal amount, WalletLogEnum logEnum);

    /**
     * 传入钱包对象
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int subMoney(long userId, BigDecimal amount, WalletLogEnum logEnum);

}

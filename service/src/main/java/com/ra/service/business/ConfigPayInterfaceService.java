package com.ra.service.business;

import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.repository.business.WalletRepository;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.base.BaseService;
import org.springframework.transaction.annotation.Transactional;
import sun.security.krb5.Config;

import java.math.BigDecimal;

/**
 * @author huli
 * @time 2019-01-24
 * @description
 */
public interface ConfigPayInterfaceService extends BaseService<ConfigPayInterfaceRepository> {

    /**
     * 根据userId获取钱包
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int addMoney(long configPayInterfaceId, BigDecimal amount, String description);

    /**
     * 传入钱包对象
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int subMoney(long configPayInterfaceId, BigDecimal amount, String description);

}

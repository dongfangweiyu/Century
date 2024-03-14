package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.entity.business.BehalfBankCardWalletLog;
import com.ra.dao.repository.business.BehalfBankCardWalletLogRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.WalletLogReq;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface BehalfBankCardWalletLogService extends BaseService<BehalfBankCardWalletLogRepository> {
    /**
     * 根据银行卡ID获取卡信息
     * 保存银行卡金额变动日记
     * @return
     */
    boolean saveBehalfBankCardLog(long bankCardId, BigDecimal amount, BehalfWalletLogEnum behalfWalletLogEnum);

    /**
     * 卡商卡组中银行卡的金额变动日志
     */
    Page<BehalfBankCardWalletLog> findBankCardLogList(WalletLogReq walletLogReq, long behalfuserId, Pager pager);

    /**
     * 卡变动金额统计
     * @param walletLogReq
     * @return
     */
    BigDecimal findByBehalfBankCardTotal(WalletLogReq walletLogReq,long behalfUserId);
}

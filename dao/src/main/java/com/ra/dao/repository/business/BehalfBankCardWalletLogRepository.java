package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfBankCardWalletLog;
import org.springframework.stereotype.Repository;

/**
 * 代付银行卡资金明细变动Dao
 */
@Repository
public interface BehalfBankCardWalletLogRepository extends BaseRepository<BehalfBankCardWalletLog,Long> {
}

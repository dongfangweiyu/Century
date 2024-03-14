package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.WalletLog;
import org.springframework.stereotype.Repository;

/**
 * 钱包变动日志Dao
 */
@Repository
public interface WalletLogRepository extends BaseRepository<WalletLog,Long> {

}

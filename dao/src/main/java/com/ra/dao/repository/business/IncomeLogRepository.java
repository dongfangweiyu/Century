package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.IncomeLog;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeLogRepository extends BaseRepository<IncomeLog,Long> {

}

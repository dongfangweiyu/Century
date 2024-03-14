package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BankList;
import org.springframework.stereotype.Repository;

@Repository
public interface BankListRepository extends BaseRepository<BankList,Long> {
    BankList findById(long id);
}

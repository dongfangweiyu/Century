package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BankCard;
import org.springframework.stereotype.Repository;

@Repository
public interface BankCardRepository extends BaseRepository<BankCard,Long> {
    BankCard findById(long id);
}

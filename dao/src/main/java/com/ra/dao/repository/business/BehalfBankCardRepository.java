package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfBankCard;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 代付银行卡Dao
 */
@Repository
public interface BehalfBankCardRepository extends BaseRepository<BehalfBankCard,Long> {
    BehalfBankCard findById(long id);

    /**
     * 扣除余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_bankCard set balance=balance - :amount where id=:id ",nativeQuery = true)
    int subMoney(@Param("amount") BigDecimal amount, @Param("id") long id);

    /**
     * 增加余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_bankCard set balance=balance + :amount where id=:id ",nativeQuery = true)
    int addMoney(@Param("amount") BigDecimal amount, @Param("id") long id);
}

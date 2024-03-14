package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.Wallet;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 钱包Dao
 */
@Repository
public interface WalletRepository extends BaseRepository<Wallet,Long> {

    Wallet findWalletById(Long id);

    Wallet findWalletByUserId(Long userId);


    /**
     * 扣除余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_wallet set money=money - :amount where userId=:userId and money - :amount >=0",nativeQuery = true)
    int subMoney(@Param("amount") BigDecimal amount, @Param("userId") long userId);

    /**
     * 加余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_wallet set money=money + :amount where userId=:userId ",nativeQuery = true)
    int addMoney(@Param("amount") BigDecimal amount, @Param("userId") long userId);

}

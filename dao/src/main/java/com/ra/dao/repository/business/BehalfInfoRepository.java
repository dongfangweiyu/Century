package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


/**
 * 卡商Dao
 */
@Repository
public interface BehalfInfoRepository extends BaseRepository<BehalfInfo,Long> {
    BehalfInfo findByUserId(Long userId);
    BehalfInfo findByAppId(String appId);
    /**
     * 扣除余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_info set profit=profit - :amount where userId=:userId and profit - :amount >=0",nativeQuery = true)
    int subMoney(@Param("amount") BigDecimal amount, @Param("userId") long userId);

    /**
     * 加余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_info set profit=profit + :amount where userId=:userId ",nativeQuery = true)
    int addMoney(@Param("amount") BigDecimal amount, @Param("userId") long userId);
}

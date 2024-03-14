package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.PayChannel;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Repository
public interface PayChannelRepository extends BaseRepository<PayChannel,Long> {

    PayChannel findById(long id);


    /**
     * 增加信誉分
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_payChannel set creditScore=creditScore + :creditScore where id=:id ",nativeQuery = true)
    int addCreditScore(@Param("id") long id, @Param("creditScore") BigDecimal creditScore);


    /**
     * 减少信誉分
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_payChannel set creditScore=creditScore - :creditScore where id=:id ",nativeQuery = true)
    int subCreditScore(@Param("id") long id, @Param("creditScore") BigDecimal creditScore);

}
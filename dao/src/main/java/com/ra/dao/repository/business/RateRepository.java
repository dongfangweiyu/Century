package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.Rate;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RateRepository extends BaseRepository<Rate,Long> {
    Rate findRateById(long id);

    Rate findRateByMerchantInfoIdAndPayChannelId(long merchantInfoId,long payChannelId);

    List<Rate> findByMerchantInfoId(long merchantInfoId);

    @Modifying
    @Query(value = "delete from  business_rate where merchantInfoId=:merchantInfoId",nativeQuery = true)
    @Transactional
    void deleteByMerchantInfoId(@Param("merchantInfoId") long merchantInfoId);
}

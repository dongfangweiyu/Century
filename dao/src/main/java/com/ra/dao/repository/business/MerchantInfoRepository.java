package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.MerchantInfo;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

/**
 * 公告Dao
 */
@Repository
public interface MerchantInfoRepository extends BaseRepository<MerchantInfo,Long> {
    MerchantInfo findByUserId(Long userId);
    MerchantInfo findByAppId(String appId);
    MerchantInfo findMerchantInfoById(Long id);

    /**
     * 审核商户信息是否通过
     * @param id
     * @return
     */
    @Modifying
    @Query(value = "update business_merchant_info set applyStatus = :applyStatus where id = :id and applyStatus='INAUDIT' ", nativeQuery = true)
    @Transactional
    int applyMerchangInfoPass(@Param("id") Long id,@Param("applyStatus") String applyStatus);
}

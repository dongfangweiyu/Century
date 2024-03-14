package com.ra.dao.repository.config;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.channel.PayInterfaceEnum;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.dao.entity.config.ConfigSystem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 第三方接口配置Dao
 * @author ouyan
 *
 */
@Repository
public interface ConfigPayInterfaceRepository extends BaseRepository<ConfigPayInterface,Long> {

    List<ConfigPayInterface> findByPayInterfaceEnum(PayInterfaceEnum payInterfaceEnum);

    ConfigPayInterface findById(long id);

    /**
     * 扣除余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update config_payInterface set money=money - :amount where id=:id and money - :amount >=0",nativeQuery = true)
    int subMoney(@Param("amount") BigDecimal amount, @Param("id") long id);

    /**
     * 扣除余额
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update config_payInterface set money=money + :amount where id=:id ",nativeQuery = true)
    int addMoney(@Param("amount") BigDecimal amount, @Param("id") long id);
}
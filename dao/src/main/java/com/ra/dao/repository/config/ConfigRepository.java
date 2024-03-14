package com.ra.dao.repository.config;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.config.ConfigSystem;
import org.springframework.stereotype.Repository;

/**
 * 系统配置Dao
 * 此dao不创建server层
 * 使用ConfigFactory维护
 * @author ouyan
 *
 */
@Repository
public interface ConfigRepository extends BaseRepository<ConfigSystem,Long> {

    ConfigSystem findByCKey(String key);


}
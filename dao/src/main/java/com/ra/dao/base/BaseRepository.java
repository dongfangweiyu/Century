package com.ra.dao.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * BaseRepository
 * 基类Repository
 * @param <T>
 * @param <ID>
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity,ID> extends JpaRepository<T,ID> {

    /**
     * 逻辑删除
     * */
    void logicDelete(ID id);
    /**
     * 批量逻辑删除
     * */
    void logicDelete(Iterable<ID> ids);
    /**
     * 批量逻辑删除
     * */
    void logicBatchDelete(Iterable<T> t);
}

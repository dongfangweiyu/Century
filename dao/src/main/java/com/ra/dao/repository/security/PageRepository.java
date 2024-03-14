package com.ra.dao.repository.security;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.security.Page;
import org.springframework.stereotype.Repository;

/**
 * @author huli
 * @time 2019-01-15
 * @description
 */
@Repository
public interface PageRepository extends BaseRepository<Page, Long> {

    Page findPageById(Long id);
}

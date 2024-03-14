package com.ra.service.security;

import com.ra.dao.entity.security.Page;
import com.ra.dao.repository.security.PageRepository;
import com.ra.service.base.BaseService;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-16
 * @description
 */
public interface PageService extends BaseService<PageRepository> {

    /**
     * 查询
     * @param keyword
     * @return
     */
    List<Page> findListPage(String keyword);

}

package com.ra.service.security.impl;

import com.ra.dao.entity.security.Page;
import com.ra.dao.repository.security.NavigationRepository;
import com.ra.dao.repository.security.PageRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.security.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huli
 * @time 2019-01-16
 * @description
 */
@Service
public class PageServiceImpl extends BaseServiceImpl<PageRepository> implements PageService {

    @Override
    public List<Page> findListPage(String keyword) {
        Map<String, Object> param = new HashMap<>();
        String sql = "select * from security_page where 1 = 1";
        if (!StringUtils.isEmpty(keyword)) {
            sql += " and name like :keyword";
            param.put("keyword", "%" + keyword + "%");
        }

        List<Page> pageList = findList(sql, param, Page.class);

        return pageList;
    }

}

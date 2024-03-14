package com.ra.service.bean.security;

import com.ra.dao.entity.security.Navigation;
import com.ra.dao.entity.security.Page;
import lombok.Data;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-15
 * @description
 */
@Data
public class NavigationVo extends Navigation {


    /**
     * 子页面
     */
    private List<NavigationVo> children;


    private String name;

    /**
     * 相对路径
     */
    private String href;

    private String icon;

    /**
     * 角色名
     */
    private String roleName;
}

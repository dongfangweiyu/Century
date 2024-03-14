package com.ra.service.security;

import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.repository.security.NavigationRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.security.NavigationVo;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-15
 * @description
 */
public interface NavigationService extends BaseService<NavigationRepository> {


    /**
     * 通过角色id查询
     *
     * @param roleId
     * @return
     */
    List<NavigationVo> findNavigation(Integer roleId, AgencyEnum agencyEnum);

    /**
     * 删除导航
     * @param roleId
     * @param agencyEnum
     * @param pageId
     * @return
     */
    boolean deleteNavigation(Long roleId,AgencyEnum agencyEnum,Long pageId);

    /**
     * 通过角色和父目录查询
     *
     * @return
     */
    List<NavigationVo> findNavigationByParent(Long parentId);


}

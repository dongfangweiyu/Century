package com.ra.service.security.impl;

import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.repository.security.NavigationRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.security.NavigationVo;
import com.ra.service.security.NavigationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huli
 * @time 2019-01-15
 * @description
 */
@Service
public class NavigationServiceImpl extends BaseServiceImpl<NavigationRepository> implements NavigationService {

    /**
     * 通过角色id查询
     *
     * @param roleId
     * @return
     */
    @Override
    public List<NavigationVo> findNavigation(Integer roleId, AgencyEnum agencyEnum) {
        Map<String, Object> param = new HashMap<>();
        String sql = "select a.*,b.href,b.name from security_nav as a left join security_page as b on a.pageId=b.id where a.parentId is null ";
        if(roleId!=null&&roleId>0){
            sql+=" and a.roleId = :roleId ";
            param.put("roleId", roleId);
        }
        if(agencyEnum!=null){
            sql+=" and a.agencyEnum=:agencyEnum ";
            param.put("agencyEnum",agencyEnum.toString());
        }
        List<NavigationVo> list = findList(sql, param, NavigationVo.class);
        for (NavigationVo navigationVo : list) {
            List<NavigationVo> childrens = findNavigationByRoleIdAndParent(navigationVo.getId().intValue());
            navigationVo.setChildren(childrens);
        }
        return list;
    }

    @Override
    public boolean deleteNavigation(Long roleId, AgencyEnum agencyEnum, Long pageId) {



        return false;
    }

    @Override
    public List<NavigationVo> findNavigationByParent(Long parentId) {
        String sql = "select a.*,b.href,b.name from security_nav as a left join security_page as b on a.pageId=b.id where a.parentId="+parentId;
        return findList(sql,new HashMap<>(),NavigationVo.class);
    }


    /**
     * 通过角色和父目录查询
     *
     * @param parentId
     * @return
     */
//    @Override
    private List<NavigationVo> findNavigationByRoleIdAndParent(Integer parentId) {
        String sql = "select a.*,b.href,b.name from security_nav as a left join security_page as b on a.pageId=b.id where a.parentId = :parentId";
        Map<String, Object> param = new HashMap<>();
        param.put("parentId", parentId);
        return findList(sql, param, NavigationVo.class);
    }

}

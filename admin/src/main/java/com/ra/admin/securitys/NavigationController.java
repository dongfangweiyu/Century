package com.ra.admin.securitys;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.security.Navigation;
import com.ra.dao.entity.security.Page;
import com.ra.dao.entity.security.Role;
import com.ra.service.bean.security.NavigationVo;
import com.ra.service.security.NavigationService;
import com.ra.service.security.PageService;
import com.ra.service.security.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huli
 * @time 2019-01-15
 * @description
 */
@Controller
@RequestMapping("/private/nav/")
public class NavigationController {

    private static final Logger logger = LoggerFactory.getLogger(NavigationController.class);

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PageService pageService;

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public String list(Model model) {
        List<Role> all = roleService.getRepository().findAll();
        model.addAttribute("roles", all);
        return "security/nav/list";
    }

    @RequestMapping(value = "/navigation", method = RequestMethod.GET)
    public String navigation(Model model,
                             @RequestParam(value = "roleId",required = false,defaultValue = "0") Integer roleId,
                             @RequestParam("agencyEnum") AgencyEnum agencyEnum) {
        List<NavigationVo> result = navigationService.findNavigation(roleId,agencyEnum);

        JSONArray nodes = new JSONArray();
        for (NavigationVo navigationVo : result) {
            JSONObject item = new JSONObject();
            item.put("id", String.valueOf(navigationVo.getId()));
            item.put("title", navigationVo.getTitle());
            item.put("spread",true);
            item.put("parent",true);

            List<NavigationVo> children = navigationVo.getChildren();
            JSONArray childNodes = new JSONArray();
            for (NavigationVo child : children) {
                JSONObject childItem = new JSONObject();
                childItem.put("id", String.valueOf(child.getId()));
                childItem.put("title", child.getTitle());
                childItem.put("parent",false);
                childNodes.add(childItem);
            }
            item.put("children", childNodes);
            nodes.add(item);
        }

        model.addAttribute("navigation", nodes);
        model.addAttribute("roleId", roleId);
        model.addAttribute("agencyEnum",agencyEnum.toString());
        return "security/nav/navigation";
    }


    @RequestMapping("/selectPage")
    @ResponseBody
    public ApiResult selectPage(@RequestParam Long id){
        ApiResult result=new ApiResult();
        try {
            List<NavigationVo> navigationByParent = navigationService.findNavigationByParent(id);
            List<Page> listPage = pageService.findListPage(null);
            JSONArray pages=new JSONArray();
            for (Page page : listPage) {
                JSONObject item = new JSONObject();
                item.put("id", String.valueOf(page.getId()));
                item.put("title", page.getName());
                item.put("parentId",id);

                for (NavigationVo navigationVo : navigationByParent) {
                    if(navigationVo.getPageId().longValue()==page.getId().longValue()){
                        item.put("checked",true);
                        item.put("navId",navigationVo.getId());
                        break;
                    }
                }
                pages.add(item);
            }
            result.put("allPages",pages);
            return result.ok();
        }catch (Exception e){
            return new ApiResult(-1,e.getMessage());
        }
    }

    @RequestMapping("/add")
    @ResponseBody
    public ApiResult add(@ModelAttribute Navigation navigation){

        try {
            if(navigation.getPageId()!=null){
                Page pageById = pageService.getRepository().findPageById(navigation.getPageId());
                Assert.notNull(pageById,"页面不存在");
                navigation.setTitle(pageById.getName());
            }
            navigationService.getRepository().save(navigation);
            return new ApiResult().ok();
        }catch (Exception e){
            return new ApiResult(-1,e.getMessage());
        }
    }


    @RequestMapping("/del")
    @ResponseBody
    public ApiResult del(@RequestParam Long id){
        try {
            navigationService.getRepository().deleteById(id);
            return new ApiResult().ok();
        }catch (Exception e){
            return new ApiResult(-1,e.getMessage());
        }
    }

}

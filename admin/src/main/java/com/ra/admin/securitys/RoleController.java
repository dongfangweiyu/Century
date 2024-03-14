package com.ra.admin.securitys;

import com.ra.common.base.ApiResult;
import com.ra.dao.entity.security.Role;
import com.ra.service.security.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-11
 * @description
 */
@Controller
@RequestMapping("/private/role")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String roleManager (Model model) {
        List<Role> roleList = roleService.getRepository().findAll();
        model.addAttribute("roleList", roleList);
        return "security/role/list";
    }

//    @RequestMapping(value = "/update", method = RequestMethod.POST)
//    @ResponseBody
//    public ApiResult updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
//        ApiResult apiResult = new ApiResult();
//        try {
//            int count = roleService.UpdateStatus(id, status);
//            if (count < 1) {
//                return apiResult.fail("更新失败");
//            }
//            return apiResult.ok();
//        } catch (Exception e) {
//            logger.error("更新状态异常 == {}", e.getMessage());
//            return apiResult.fail("更新状态异常");
//        }
//    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("role", new Role()).addAttribute("_method", "POST");
        return "security/role/form";
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String create(@ModelAttribute Role role) {
        Assert.hasText(role.getName(), "角色名不能为空");
        Assert.hasText(role.getDescription(), "描述不能为空");
        try {
            role.setStatus(0);
            roleService.getRepository().save(role);
            return "redirect:/private/role/list";
        } catch (Exception e) {
            logger.error("添加角色异常", e);
            return "security/role/form";
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String editRole(@RequestParam("id") Long id, Model model) {
        Role role = roleService.getRepository().findRoleById(id);
        Assert.notNull(role, "查询不到该角色");
        model.addAttribute("role", role).addAttribute("_method", "PUT");
        return "security/role/form";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT)
    public String editRole(@ModelAttribute Role role) {
        Assert.hasText(role.getName(), "角色名不能为空");
        Assert.hasText(role.getDescription(), "描述不能为空");
        try {
            roleService.getRepository().save(role);
            return "redirect:/private/role/list";
        } catch (Exception e) {
            logger.error("修改角色异常", e);
            return "security/role/form";
        }
    }

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult remove(@RequestParam("id") Long id) {
        ApiResult apiResult = new ApiResult();
        try {
            Role role = roleService.getRepository().findRoleById(id);
            Assert.notNull(role,"角色不存在");
            roleService.getRepository().delete(role);
            apiResult.ok();
        } catch (Exception e) {
            logger.error("角色删除异常", e);
            apiResult.fail("角色删除异常");
        }
        return apiResult;
    }
}

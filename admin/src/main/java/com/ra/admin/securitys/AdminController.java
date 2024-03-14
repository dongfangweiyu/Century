package com.ra.admin.securitys;

import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.security.Role;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.resp.UserAdminVo;
import com.ra.service.security.RoleService;
import com.ra.service.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-17
 * @description
 */
@Controller
@RequestMapping("/private/admin")
public class AdminController {


    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(Model model) {
        List<UserAdminVo> userByAgency = userService.findUserByAgency(AgencyEnum.ADMIN);
        model.addAttribute("page", userByAgency);
        return "security/admin/list";
    }


    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model) {
        List<Role> all = roleService.getRepository().findAll();
        model.addAttribute("user", new User())
                .addAttribute("roles", all).addAttribute("_method", "POST");

        return "security/admin/form";
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String create(@ModelAttribute User user, Model model) {
        try {
            Assert.hasText(user.getAccount(),"请输入账号");
            Assert.hasText(user.getPassword(),"请输入密码");
            Assert.notNull(user.getRoleID(),"请选择角色");

            boolean b = userService.addAdminUser(user);
            if(!b){
                throw new IllegalArgumentException("fail");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
            List<Role> all = roleService.getRepository().findAll();
            model.addAttribute("roles", all).addAttribute("_method", "POST");
            model.addAttribute("user", user).addAttribute("message", ex.getMessage());
            return "security/admin/form";
        }

        return "redirect:/private/admin/list";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(Model model, @RequestParam("id") Long id) {
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"用户不存在");
        List<Role> all = roleService.getRepository().findAll();

        User template=new User();
        template.setAccount(user.getAccount().substring(0,user.getAccount().indexOf("@")));
        template.setId(user.getId());
        template.setRoleID(user.getRoleID());
        model.addAttribute("user", template).addAttribute("_method", "PUT").addAttribute("roles", all);
        return "security/admin/form";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT)
    public String edit(@ModelAttribute User user,Model model) {
        try {
            Assert.hasText(user.getAccount(),"请输入账号");
            Assert.notNull(user.getRoleID(),"请选择角色");

            User old = userService.getRepository().findUserById(user.getId());
            old.setAccount(user.getAccount()+"@admin.com");
            old.setRoleID(user.getRoleID());
            userService.getRepository().save(old);
            return "redirect:/private/admin/list";
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
            List<Role> all = roleService.getRepository().findAll();
            model.addAttribute("roles", all).addAttribute("_method", "PUT");
            model.addAttribute("user", user).addAttribute("message", ex.getMessage());
            return "security/admin/form";
        }
    }

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult remove(@RequestParam("id") Long id) {
        ApiResult apiResult = new ApiResult();
        try {
            userService.getRepository().deleteById(id);
            apiResult.ok();
        } catch (Exception e) {
            logger.error("[删除异常]", e);
            apiResult.fail("删除异常");
        }
        return apiResult;
    }

    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        try {
            User user = userService.getRepository().findUserById(id);
            if(user.getStatus()==0){
                userService.getRepository().updateStatus(id,1);
            }else
            if(user.getStatus()==1){
                userService.getRepository().updateStatus(id,0);
            }
        } catch (Exception e) {
            logger.error("操作失败 == {}", e);
        }
        return "redirect:/private/admin/list";
    }

    /**
     * 修改密码
     *
     * @return
     */
    @RequestMapping(value = "/reset", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult reset(@RequestParam("id") long id) {
        ApiResult apiResult = new ApiResult();
        try {
            boolean b = userService.resetPassword(id);
            if(b){
               return apiResult.ok("操作成功");
            }
        } catch (Exception e) {
            logger.error("修改密码失败 == {}", e);
            return apiResult.fail("修改密码失败");
        }
        return apiResult.fail("操作失败");
    }

    /**
     * 重置支付密码
     *
     * @return
     */
    @RequestMapping(value = "/resetPayPassword", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult resetPayPassword(@RequestParam("id") long id) {
        ApiResult apiResult = new ApiResult();
        try {
            boolean b = userService.resetPayPassword(id);
            if(b){
                return apiResult.ok("操作成功");
            }
        } catch (Exception e) {
            logger.error("重置支付密码失败 == {}", e);
            return apiResult.fail("重置支付密码失败");
        }
        return apiResult.fail("操作失败");
    }


}

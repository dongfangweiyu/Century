package com.ra.admin.controller.admin;


import com.ra.common.base.ApiResult;
import com.ra.service.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/private/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    /**
     * 解绑谷歌验证
     * @param id
     * @return
     */
    @PostMapping("/updateUsergoogle")
    @ResponseBody
    public ApiResult updateUsergoogle(@RequestParam("id") long id){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(id, "用户id不能为空");
            int confirm = userService.getRepository().updateUsergoogle(id);
            if(confirm>0){
                return result.ok();
            }
            return result.fail();
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }
    }
}

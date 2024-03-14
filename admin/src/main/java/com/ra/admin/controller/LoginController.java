package com.ra.admin.controller;

import com.baomidou.kaptcha.Kaptcha;
import com.baomidou.kaptcha.exception.KaptchaIncorrectException;
import com.baomidou.kaptcha.exception.KaptchaNotFoundException;
import com.baomidou.kaptcha.exception.KaptchaTimeoutException;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.component.GoogleAuthenticator;
import com.ra.common.exception.TokenException;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.security.NavigationVo;
import com.ra.service.security.NavigationService;
import com.ra.service.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @author huli
 */
@Controller("/")
public class LoginController {

    Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private Kaptcha kaptcha;

    @Autowired
    private UserService userService;
    @Autowired
    private NavigationService navigationService;

    public static void main(String[] args) {
//        String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("123456"));
//        System.out.println(passwordAsMd5);
//        String qrcode = GoogleAuthenticator.getQRBarcode("战斗机@aa.com",secret );
//        System.out.println(qrcode);
    }

    @RequestMapping(value = "/")
    public String layout(Model model, HttpServletResponse response, HttpServletRequest request) {
        boolean loginFlag = TokenUtil.verifyLoginInfo(response);//验证登录的身份
        if (loginFlag) {
            User loginInfo = TokenUtil.getLoginInfo();
            model.addAttribute("user", loginInfo);

//            /*添加菜单*/
            List<NavigationVo> result = navigationService.findNavigation(loginInfo.getRoleID(),loginInfo.getAgencyType());
            model.addAttribute("navigation", result);
            return "index";
        }
        return "login";
    }

    @GetMapping(value = "/public/login")
    public String login() {
        return "redirect:/";
    }

    @PostMapping(value = "/public/login")
    public String login(Model model,
                        @RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String validateCode,
                        @RequestParam(required = false) Long googleAuthenticator,
                        HttpServletResponse response,
                        HttpServletRequest request
    ) {
        try {
            Assert.hasText(username,"请输入用户名");
            Assert.hasText(password,"请输入密码");
            Assert.hasText(validateCode,"请输入验证码");

            boolean validate = kaptcha.validate(validateCode);
            Assert.isTrue(validate,"验证码错误");

            String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(password));
            User user = userService.loginAdmin(request,username, passwordAsMd5);

            //如果开启了二次验证
            if(!StringUtils.isEmpty(user.getGoogleAuthenticator())){
                Assert.notNull(googleAuthenticator,"请输入谷歌验证码");
                long t = System.currentTimeMillis();
                GoogleAuthenticator ga = new GoogleAuthenticator();
                ga.setWindowSize(5);
                boolean r = ga.check_code(user.getGoogleAuthenticator(), googleAuthenticator, t);
                if(!r){
                    throw new IllegalArgumentException("谷歌验证码输入有误");
                }
            }

            String host = IpUtil.getHost(request);
            if(host.toLowerCase().contains("daifu2.com")&&user.getAgencyType()!= AgencyEnum.BEHALF){
                throw new IllegalArgumentException("账号不存在");
            }

            TokenUtil.setLoginInfo(response, user);//保存登录信息到Cookie
            return "redirect:/";
        }catch (KaptchaIncorrectException e){
            model.addAttribute("message", "验证码不正确");
            return "login";
        }catch (KaptchaNotFoundException e){
            model.addAttribute("message","验证码未找到");
            return "login";
        }catch (KaptchaTimeoutException e){
            model.addAttribute("message","验证码已过期");
            return "login";
        }catch (Exception e) {
            logger.error(e.getMessage(), e);
            model.addAttribute("message", e.getMessage());
            return "login";
        }
    }

    @GetMapping(value = "/public/kaptcha")
    @ResponseBody
    public void kaptcha(){
       kaptcha.render();
    }


    @RequestMapping("/public/loginOut")
    public String loginOut(Model model, HttpServletResponse response) {
        TokenUtil.removeLoginInfo(response);
        model.addAttribute("message", "退出成功");
        return "redirect:/";//重定向
    }

    @GetMapping("/private/modifyPassword")
    public Object modifyPassword() {
        return "security/admin/modifypassword";
    }

    @PostMapping("/private/modifyPassword")
    @ResponseBody
    public Object modifyPassword(HttpServletRequest request, String oldPassword, String password) {
        ApiResult apiResult = new ApiResult();
        try {
            Assert.notNull(oldPassword, "原密码不能为空");
            Assert.notNull(password, "新密码不能为空");

            Long loginId = TokenUtil.getLoginId();
            User findUserById = userService.getRepository().findUserById(loginId);
            String oldMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(oldPassword));
            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(password));
            if (!findUserById.getPassword().equals(oldMd5)) {
                throw new IllegalArgumentException("原密码输入错误!");
            }
            findUserById.setPassword(md5);
            userService.getRepository().save(findUserById);
            return apiResult.ok("修改成功,请重新登录");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return apiResult.fail(e.getMessage());
        }
    }

    /**
     * 弹出开通支付密码
     * @return
     */
    @GetMapping("/private/addPayPassword")
    public Object addPayPassword() {
        return "security/admin/addPayPassword";
    }

    /**
     * 新增支付密码
     * @param request
     * @param payPassword
     * @return
     */
    @PostMapping("/private/addPayPassword")
    @ResponseBody
    public Object addPayPassword(HttpServletRequest request, String payPassword) {
        ApiResult apiResult = new ApiResult();
        try {
            Assert.notNull(payPassword, "支付密码不能为空");
            Long loginId = TokenUtil.getLoginId();
            User findUserById = userService.getRepository().findUserById(loginId);
            if (!StringUtils.isEmpty(findUserById.getPay_password())) {
                throw new IllegalArgumentException("您已开通支付密码！");
            }
            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(payPassword));
            findUserById.setPay_password(md5);
            userService.getRepository().save(findUserById);
            return apiResult.ok();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return apiResult.fail(e.getMessage());
        }
    }

    /**
     * 弹出修改支付密码
     * @return
     */
    @GetMapping("/private/modifyPayPassword")
    public Object modifyPayPassword() {
        return "security/admin/modifyPayPassword";
    }

    /**
     * 修改支付密码
     * @param request
     * @param oldPayPassword
     * @param payPassword
     * @return
     */
    @PostMapping("/private/modifyPayPassword")
    @ResponseBody
    public Object modifyPayPassword(HttpServletRequest request, String oldPayPassword, String payPassword) {
        ApiResult apiResult = new ApiResult();
        try {
            Assert.notNull(oldPayPassword, "原支付密码不能为空");
            Assert.notNull(payPassword, "新支付密码不能为空");

            Long loginId = TokenUtil.getLoginId();
            User findUserById = userService.getRepository().findUserById(loginId);
            String oldMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(oldPayPassword));
            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(payPassword));
            if (!oldMd5.equals(findUserById.getPay_password())) {
                throw new IllegalArgumentException("原支付密码输入错误!");
            }
            findUserById.setPay_password(md5);
            userService.getRepository().save(findUserById);
            return apiResult.ok();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return apiResult.fail(e.getMessage());
        }
    }


//
//    /**
//     * 弹出开通支付密码
//     * @return
//     */
//    @GetMapping("/private/addPayPassword")
//    public Object addPayPassword() {
//        return "security/admin/addPayPassword";
//    }
//
//    /**
//     * 新增支付密码
//     * @param request
//     * @param payPassword
//     * @return
//     */
//    @PostMapping("/private/addPayPassword")
//    @ResponseBody
//    public Object addPayPassword(HttpServletRequest request, String payPassword) {
//        ApiResult apiResult = new ApiResult();
//        try {
//            Assert.notNull(payPassword, "支付密码不能为空");
//            Long loginId = TokenUtil.getLoginId();
//            User findUserById = userService.getRepository().findUserById(loginId);
//            if (!StringUtils.isEmpty(findUserById.getPay_password())) {
//                throw new IllegalArgumentException("您已开通支付密码！");
//            }
//            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(payPassword));
//            findUserById.setPay_password(md5);
//            userService.getRepository().save(findUserById);
//            return apiResult.ok();
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            return apiResult.fail(e.getMessage());
//        }
//    }
//
//    /**
//     * 弹出修改支付密码
//     * @return
//     */
//    @GetMapping("/private/modifyPayPassword")
//    public Object modifyPayPassword() {
//        return "security/admin/modifyPayPassword";
//    }
//
//    /**
//     * 修改支付密码
//     * @param request
//     * @param oldPayPassword
//     * @param payPassword
//     * @return
//     */
//    @PostMapping("/private/modifyPayPassword")
//    @ResponseBody
//    public Object modifyPayPassword(HttpServletRequest request, String oldPayPassword, String payPassword) {
//        ApiResult apiResult = new ApiResult();
//        try {
//            Assert.notNull(oldPayPassword, "原支付密码不能为空");
//            Assert.notNull(payPassword, "新支付密码不能为空");
//
//            Long loginId = TokenUtil.getLoginId();
//            User findUserById = userService.getRepository().findUserById(loginId);
//            String oldMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(oldPayPassword));
//            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(payPassword));
//            if (!oldMd5.equals(findUserById.getPay_password())) {
//                throw new IllegalArgumentException("原支付密码输入错误!");
//            }
//            findUserById.setPay_password(md5);
//            userService.getRepository().save(findUserById);
//            return apiResult.ok();
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            return apiResult.fail(e.getMessage());
//        }
//    }


    //当测试authTest时候，把genSecretTest生成的secret值赋值给它
//    private static String secret="R2Q3S52RNXBTFTOM";

    @GetMapping("/private/googleAuthenticator")
    public String googleAuthenticator(Model model, HttpSession session) {
        Object secret = session.getAttribute("secret");
        if(StringUtils.isEmpty(secret)){
            secret = GoogleAuthenticator.generateSecretKey();
            session.setAttribute("secret",secret);
        }

        Long loginId = TokenUtil.getLoginId();
        User user = userService.getRepository().findUserById(loginId);
        String qrcode = GoogleAuthenticator.getQRBarcode(user.getAccount(),secret.toString());
        model.addAttribute("open", !StringUtils.isEmpty(user.getGoogleAuthenticator()));
        model.addAttribute("qrcode",qrcode);
        return "security/admin/googleAuthenticator";
    }

    @PostMapping("/private/googleAuthenticator")
    public String saveGoogleAuthenticator(Model model,HttpSession session,
                                          @RequestParam Long validateCode) {
        try{
            Object secret=session.getAttribute("secret");
            Assert.notNull(secret,"google秘钥不能为空");
            Assert.notNull(validateCode,"谷歌验证码不能为空");

            long t = System.currentTimeMillis();
            GoogleAuthenticator ga = new GoogleAuthenticator();
            ga.setWindowSize(5);
            boolean r = ga.check_code(secret.toString(), validateCode, t);
            if(!r){
                throw new IllegalArgumentException("谷歌验证码输入错误");
            }

            Long loginId = TokenUtil.getLoginId();
            User user = userService.getRepository().findUserById(loginId);
            if(!StringUtils.isEmpty(user.getGoogleAuthenticator())){
                throw new IllegalArgumentException("已经绑定过了");
            }
            user.setGoogleAuthenticator(secret.toString());
            userService.getRepository().save(user);
            return "redirect:/private/googleAuthenticator";
        }catch (Exception e){
            googleAuthenticator(model,session);
            model.addAttribute("message",e.getMessage());
            return "security/admin/googleAuthenticator";
        }
    }

    /**
     * 账号是否开启谷歌验证
     * @param userName
     * @return
     */
    @PostMapping("/public/isNoGoogle")
    @ResponseBody
    public ApiResult isNoGoogle(@RequestParam String userName) {
        ApiResult result=new ApiResult();
        try{
            Assert.hasText(userName,"请先输入登录账号");
            User user=userService.getRepository().findUserByAccount(userName);
            if(user !=null && !StringUtils.isEmpty(user.getGoogleAuthenticator())){
                System.out.println("该账户有谷歌验证！！！！");
                return result.ok();
            }
            return result.fail();
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }
    }
}

package com.ra.admin.controller.admin;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.security.User;
import com.ra.service.business.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
@RequestMapping("/private/wallet")
public class WalletController {
    @Autowired
    WalletService walletService;

    /**
     * 修改金额
     * @param userId 用户id
     * @param walletMoney 修改的账户余额
     * @return
     */
    @PostMapping("/modifyWalletMoney")
    @ResponseBody
    public ApiResult modifyWalletMoney(@RequestParam("userId") Long userId,
                                       @RequestParam("walletMoney") BigDecimal walletMoney,
                                       @RequestParam(value = "remark",required = false,defaultValue = "")String remark){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(userId, "用户id不能为空");
            Assert.notNull(walletMoney, "修改的金额不能为空");

            User loginInfo = TokenUtil.getLoginInfo();
            WalletLogEnum admin = WalletLogEnum.ADMIN;
            admin.setDescription(remark+"（管理员："+loginInfo.getAccount()+"修改金额变动）");
            int i = walletService.addMoney(userId, walletMoney,admin);
            if(i>0){
                return result.ok();
            }
            return result.fail("修改失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }
}

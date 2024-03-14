package com.ra.admin.controller.proxy;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.component.GoogleAuthenticator;
import com.ra.common.domain.Pager;
import com.ra.common.utils.EncryptUtil;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.WithdrawOrderVo;
import com.ra.service.business.BankCardService;
import com.ra.service.business.WalletService;
import com.ra.service.business.WithdrawOrderService;
import com.ra.service.component.UploadComponent;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/private/proxyWorder")
public class ProxyWithdrawOrderController {
    @Autowired
    WalletService walletService;

    @Autowired
    WithdrawOrderService withdrawOrderService;

    @Autowired
    BankCardService bankCardService;

    @Autowired
    UserService userService;

    /**
     * 代理提现订单列表
     * @param withdrawOrderReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListWithdrawOrder", method = RequestMethod.GET)
    public String findListWithdrawOrder(@ModelAttribute WithdrawOrderReq withdrawOrderReq, Model model, @ModelAttribute Pager pager, HttpServletRequest request){
        long proxyUserId=TokenUtil.getLoginId();
        Page<WithdrawOrderVo> listByCondition = withdrawOrderService.findWithdrawOrderList(withdrawOrderReq,proxyUserId, pager);
        if(listByCondition!=null){
            for(WithdrawOrderVo withdrawOrderVo:listByCondition){
                BankCard bankCard= JSON.parseObject(withdrawOrderVo.getBankCardInfoJson(), BankCard.class);
                withdrawOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());
            }
        }
        Wallet wallet=walletService.findWallet(proxyUserId);//余额
        String url= UploadComponent.getPayHost(request);
        model.addAttribute("listByCondition", listByCondition).addAttribute("withdrawOrderReq", withdrawOrderReq)
                .addAttribute("wallet", wallet).addAttribute("payUrl",url)
                .addAttribute("pager", pager);
        return "business/proxy/withdrawOrder/list";

    }

    @GetMapping("/withdraw")
    public String withdraw(Model model){
        try{
            long proxyUserId=TokenUtil.getLoginId();
            User userById = userService.getRepository().findUserById(proxyUserId);
            List<BankCard>listBankCard=bankCardService.findBankCardList(proxyUserId);
            Wallet wallet=walletService.findWallet(proxyUserId);//余额
            model.addAttribute("wallet",wallet);
            model.addAttribute("listBankCard",listBankCard);
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("withdrawRate", ConfigFactory.getBigDecimal(ConfigEnum.WITHDRAW_RATE));
            model.addAttribute("openPayPassword", !StringUtils.isEmpty(userById.getPay_password()));
            return "business/proxy/withdrawOrder/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/proxyWorder/findListWithdrawOrder";
    }
    /**
     * 提现
     * @param orderMoney 提现金额
     * @param bankCardId 收款信息
     * @param fastGoogleAuthenticator 是否开启谷歌验证
     * @return
     */
    @RequestMapping(value = "/withdraw", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult withdraw(@RequestParam("orderMoney") Integer orderMoney,
                                               @RequestParam("bankCardId")long bankCardId,
                                               @RequestParam(required = false)Long fastGoogleAuthenticator,@RequestParam(value = "fastPayPassword",required=false)String fastPayPassword){
        ApiResult result=new ApiResult();
        try {
            Assert.notNull(orderMoney, "提现金额不能为空");
            Assert.notNull(bankCardId, "提现方式不能为空");
            if(orderMoney<100){
                throw new IllegalArgumentException("提现金额必须满足"+100+"元以上，谢谢！");
            }
            User userById =userService.getRepository().findUserById(TokenUtil.getLoginId());
            //如果开启了二次验证
            if(!StringUtils.isEmpty(userById.getGoogleAuthenticator())){
                Assert.notNull(fastGoogleAuthenticator,"谷歌验证随机码不能为空");
                long t = System.currentTimeMillis();
                GoogleAuthenticator ga = new GoogleAuthenticator();
                ga.setWindowSize(5);
                boolean r = ga.check_code(userById.getGoogleAuthenticator(), fastGoogleAuthenticator, t);
                if(!r){
                    throw new IllegalArgumentException("二次验证未通过");
                }
            }

            //如果开启了支付密码
            if(!StringUtils.isEmpty(userById.getPay_password())){
                Assert.notNull(fastPayPassword, "支付密码不能为空");
                String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
                if (!userById.getPay_password().equals(md5)) {
                    throw new IllegalArgumentException("支付密码输入错误!");
                }
            }

           /* //如果开启了支付密码
            if(!StringUtils.isEmpty(userById.getPay_password())){
                Assert.notNull(fastPayPassword, "支付密码不能为空");
                String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
                if (!userById.getPay_password().equals(md5)) {
                    throw new IllegalArgumentException("支付密码输入错误!");
                }
            }*/
            BigDecimal withdrawRate=ConfigFactory.getBigDecimal(ConfigEnum.WITHDRAW_RATE);//提现手续费

            Wallet walletBean=walletService.findWallet(userById.getId());
            if(BigDecimal.valueOf(orderMoney).compareTo(walletBean.getMoney().subtract(withdrawRate))==1){
                throw new IllegalArgumentException("提现金额最大为："+walletBean.getMoney().subtract(withdrawRate).toPlainString()+"元");
            }
            WithdrawlOrder withdrawlOrder=new WithdrawlOrder();
            withdrawlOrder.setMerchantUserId(userById.getId());
            withdrawlOrder.setOrderNo(withdrawlOrder.genaralOrderNo("TX"));
            withdrawlOrder.setAmount(BigDecimal.valueOf(orderMoney));
            withdrawlOrder.setStatus(OrderStatusEnum.PROCESS);
            BankCard bankCard =bankCardService.getRepository().findById(bankCardId);
           /* String[] data = selectList.split(",");//提现方式
            for (String a : data) {
                long id = Long.parseLong(a);
                bankCard = bankCardService.getRepository().findById(id);
                if(bankCard==null){
                    return result.fail("银行卡添加异常,请刷新页面");
                }
            }*/
            String payJson = JSON.toJSONString(bankCard);
            withdrawlOrder.setBankCardInfoJson(payJson);
            boolean i = withdrawOrderService.addWithdrawal(withdrawlOrder);
            if (i) {
                return result.ok();
            }
            return result.fail("提现失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }

    @PostMapping("/cancel")
    @ResponseBody
    public ApiResult cancel(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            WithdrawlOrder withdrawlOrder = withdrawOrderService.getRepository().findWithdrawlOrderById(id);
            if(!StringUtils.isEmpty(withdrawlOrder.getPaymentVoucher())){
                throw new IllegalArgumentException("状态已变更,取消失败!");
            }
            User loginInfo = TokenUtil.getLoginInfo();
            String remark="代理:"+loginInfo.getAccount()+"取消提现！";
            boolean i = withdrawOrderService.cancelWithdrawOrder(id,loginInfo.getId(),remark);
            if(i){
                return result.ok("取消成功");
            }
            return result.fail();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }

    /**
     * 确认该提现订单
     * @param id
     * @return
     */
    @PostMapping("/confirmWOrder")
    @ResponseBody
    public ApiResult confirmWOrder(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            boolean i = withdrawOrderService.compeleteOrder(id, TokenUtil.getLoginId());
            if(i){
                return result.ok("确认成功");
            }
            return result.fail();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }
}

package com.ra.admin.controller.merchant;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.business.WithdrawlOrder;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.WithdrawOrderVo;
import com.ra.service.business.WithdrawOrderService;
import com.ra.service.component.UploadComponent;
import jdk.nashorn.internal.parser.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/private/merchantWithdraw")
public class MerchantWithdrawController {
    @Autowired
    WithdrawOrderService withdrawOrderService;


    /**
     * 商户提现订单列表
     * @param withdrawOrderReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListWithdrawOrder", method = RequestMethod.GET)
    public String findListWithdrawOrder(@ModelAttribute WithdrawOrderReq withdrawOrderReq, Model model, @ModelAttribute Pager pager, HttpServletRequest request){
        long merchantUserId= TokenUtil.getLoginId();
        Page<WithdrawOrderVo> listByCondition = withdrawOrderService.findWithdrawOrderList(withdrawOrderReq,merchantUserId, pager);
        if(listByCondition!=null){
            for(WithdrawOrderVo withdrawOrderVo:listByCondition){
                BankCard bankCard= JSON.parseObject(withdrawOrderVo.getBankCardInfoJson(), BankCard.class);
                withdrawOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());
            }
        }
        String url= UploadComponent.getPayHost(request);
        model.addAttribute("listByCondition", listByCondition).addAttribute("withdrawOrderReq", withdrawOrderReq)
                .addAttribute("pager", pager).addAttribute("payUrl",url);
        return "business/merchant/withdrawOrder/list";

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
            String remark="商户:"+loginInfo.getAccount()+"取消提现！";
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

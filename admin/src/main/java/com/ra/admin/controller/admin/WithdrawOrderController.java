package com.ra.admin.controller.admin;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.business.WithdrawlOrder;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.WithdrawOrderVo;
import com.ra.service.business.WithdrawOrderService;
import com.ra.service.component.UploadComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/private/withdrawOrder")
public class WithdrawOrderController {
    @Autowired
    WithdrawOrderService withdrawOrderService;

    /**
     * 提现订单列表
     * @param withdrawOrderReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListWithdrawOrder", method = RequestMethod.GET)
    public String findListWithdrawOrder(@ModelAttribute WithdrawOrderReq withdrawOrderReq, Model model, @ModelAttribute Pager pager, HttpServletRequest request){
        pager.setSize(20);
        Page<WithdrawOrderVo> listByCondition = withdrawOrderService.findWithdrawOrderList(withdrawOrderReq,null, pager);
        if(listByCondition!=null){
            for(WithdrawOrderVo withdrawOrderVo:listByCondition){
                BankCard bankCard= JSON.parseObject(withdrawOrderVo.getBankCardInfoJson(), BankCard.class);
                withdrawOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());
            }
        }
        String url= UploadComponent.getPayHost(request);
        model.addAttribute("listByCondition", listByCondition).addAttribute("withdrawOrderReq", withdrawOrderReq)
               .addAttribute("payUrl",url).addAttribute("pager", pager);
        return "business/admin/withdrawOrder/list";

    }

    /**
     * 确认付款
     * @return
     */
    @PostMapping(value = "/confirm")
    @ResponseBody
    public ApiResult confirm(@RequestParam Long orderId,@RequestParam String paymentVoucher,
                             @RequestParam(required = false) String remark) {
        ApiResult result=new ApiResult();
        try{
            Assert.isTrue(orderId!=null&&orderId>0,"orderId:充值订单ID不能为空");
            Assert.hasText(paymentVoucher,"paymentVoucher:请上传付款凭证");
            User user= TokenUtil.getLoginInfo();
            if(user.getAgencyType()!= AgencyEnum.ADMIN){
                throw new IllegalArgumentException("只有管理员才有权限处理!");
            }
            remark="管理员:"+user.getAccount()+"确认下发,已转账";
            boolean b = withdrawOrderService.confirmOrder(orderId, paymentVoucher, remark);
            if(b){
                return result.ok();
            }
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
        return result.fail();
    }


    /**
     * 上传文件
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult uploadFile(MultipartHttpServletRequest request) {
        ApiResult result = new ApiResult();
        try {
            MultipartFile file = request.getFile("file");
            String upload = UploadComponent.upload(file, UploadComponent.UploadTypeEnum.pay);
            Map<String, Object> params = new HashMap<>();
            params.put("path", UploadComponent.getHost(request) + upload);
            params.put("fileName",upload);
            return result.ok().inject(params);
        } catch (Exception e) {
            return result.fail("上传失败");
        }
    }

    /**
     * 我处理
     * @param id
     * @return
     */
    @PostMapping("/meDeal")
    @ResponseBody
    public ApiResult meDeal(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            WithdrawlOrder withdrawlOrder=withdrawOrderService.getRepository().findWithdrawlOrderById(id);
            Assert.notNull(withdrawlOrder,"查询不到该提现订单");
            User loginInfo = TokenUtil.getLoginInfo();
            boolean i = withdrawOrderService.meDealOrder(id,loginInfo.getAccount());
            if(i){
                return result.ok("操作成功");
            }
            return result.fail();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }

    /**
     * 取消该提现订单
     * @param id
     * @return
     */
    @PostMapping("/cancel")
    @ResponseBody
    public ApiResult cancel(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            WithdrawlOrder withdrawlOrder=withdrawOrderService.getRepository().findWithdrawlOrderById(id);
            Assert.notNull(withdrawlOrder,"查询不到该提现订单");
            User loginInfo = TokenUtil.getLoginInfo();
            String remark="管理员:"+loginInfo.getAccount()+"取消下发！";
            boolean i = withdrawOrderService.cancelWithdrawOrder(id,withdrawlOrder.getMerchantUserId(),remark);
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
            WithdrawlOrder withdrawlOrder=withdrawOrderService.getRepository().findWithdrawlOrderById(id);
            Assert.notNull(withdrawlOrder,"查询不到该提现订单");
            boolean i = withdrawOrderService.compeleteOrder(id,withdrawlOrder.getMerchantUserId());
            if(i){
               return result.ok("确认成功");
            }
            return result.fail();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }
}


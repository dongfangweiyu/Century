package com.ra.admin.controller.admin;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.base.ApiResult;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.BehalfOrderReq;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.resp.BehalfOrderTotalVo;
import com.ra.service.bean.resp.BehalfOrderVo;
import com.ra.service.bean.resp.PayOrderVo;
import com.ra.service.business.BehalfOrderService;
import com.ra.service.component.BehalfOrderComponent;
import com.ra.service.component.UploadComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/private/behalfOrder")
public class BehalfOrderController {

    @Autowired
    BehalfOrderService behalfOrderService;
    @Autowired
    BehalfOrderComponent behalfOrderComponent;
    /**
     * 代付提现订单列表
     * @param behalfOrderReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListBehalfOrder", method = RequestMethod.GET)
    public String findListBehalfOrder(@ModelAttribute BehalfOrderReq behalfOrderReq, Model model, @ModelAttribute Pager pager, HttpServletRequest request){
        pager.setSize(20);
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,null,null, pager);
        if(listByCondition!=null){
            for(BehalfOrderVo behalfOrderVo:listByCondition){
                BankCard bankCard= JSON.parseObject(behalfOrderVo.getBankCardInfoJson(), BankCard.class);
                behalfOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());

                BehalfBankCard behalfBankCard= JSON.parseObject(behalfOrderVo.getBehalfBankCardInfoJson(), BehalfBankCard.class);
                behalfOrderVo.setBehalfBankCardInfo(behalfBankCard.getRealName()+'：'+behalfBankCard.getBankName()+'：'+behalfBankCard.getBankNo()+'：'+behalfBankCard.getBankBranch());
            }
        }
        BehalfOrderTotalVo behalfOrderTotal=behalfOrderService.findByBehalfOrderTotal(behalfOrderReq,null,null);
        String url= UploadComponent.getPayHost(request);
        model.addAttribute("listByCondition", listByCondition).addAttribute("behalfOrderReq", behalfOrderReq).addAttribute("behalfOrderTotal", behalfOrderTotal)
               .addAttribute("payUrl",url).addAttribute("pager", pager);
        return "business/admin/behalfOrder/list";

    }

    @PostMapping("/fixOrder")
    @ResponseBody
    public ApiResult fixOrder(@RequestParam String orderNo){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(orderNo,"订单号不能为空");
            BehalfOrder behalfOrder = behalfOrderService.getRepository().findByOrderNo(orderNo);
            Assert.notNull(behalfOrder,"代付订单信息不存在");
            User user=TokenUtil.getLoginInfo();
            if(behalfOrder.getStatus()== OrderStatusEnum.SUCCESS){
                Assert.isTrue(behalfOrder.getCompleteTime()==null,"请不要重复回调");
                //如果是成功订单，就补回调
                behalfOrderComponent.notifyOrder(behalfOrder,behalfOrder.getStatus().toString(),user.getAccount()+"手动补回调");
                return result.ok("操作成功");
            }else{
                //如果是处理中或者失败，就补单
                boolean b=behalfOrderService.compeleteBehalfOrder(behalfOrder,user);
                if(b){
                    return result.ok("补单成功");
                }
            }
            return result.fail("补单失败");
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }

    /**
     * 弹出取消订单
     * @param model
     * @return
     */
    @GetMapping("/cancel")
    public String updateMoney(Model model,@RequestParam("id") Long id){
        try{
            model.addAttribute("id",id);
            return "business/admin/behalfOrder/cancelRemark";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/behalfOrder/findListBehalfOrder";
    }
    /**
     * 取消该提现订单
     * @param id
     * @return
     */
    @PostMapping("/cancel")
    @ResponseBody
    public ApiResult cancel(@RequestParam("id") Long id, @RequestParam(value = "remark",required = false,defaultValue = "")String remark){
        ApiResult result=new ApiResult();
        try{
            User user= TokenUtil.getLoginInfo();
            if(user.getAgencyType()!= AgencyEnum.ADMIN){
                throw new IllegalArgumentException("只有管理员才有权限确认该订单!");
            }
            BehalfOrder behalfOrder=behalfOrderService.getRepository().findBehalfOrderById(id);
            Assert.notNull(behalfOrder,"查询不到该提现订单");
            behalfOrder.setRemark(remark);
            remark=user.getAccount()+"取消订单！["+remark+"]";
            boolean i = behalfOrderService.cancelBehalfOrder(behalfOrder,remark);
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
    @PostMapping("/confirmOrder")
    @ResponseBody
    public ApiResult confirmOrder(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            User user= TokenUtil.getLoginInfo();
            if(user.getAgencyType()!= AgencyEnum.ADMIN){
                throw new IllegalArgumentException("只有管理员才有权限确认该订单!");
            }
            BehalfOrder behalfOrder=behalfOrderService.getRepository().findBehalfOrderById(id);
            Assert.notNull(behalfOrder,"查询不到该提现订单");
//            Assert.notNull(behalfOrder.getPaymentVoucher(),"请联系卡商上传凭证，不然无法确认订单");
            boolean i = behalfOrderService.compeleteBehalfOrder(behalfOrder,user);
            if(i){
               return result.ok("确认成功");
            }
            return result.fail();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }

    /**
     * 导出代付订单列表
     * @param response
     * @param behalfOrderReq
     * @param pager
     * @throws Exception
     */
    @RequestMapping(value = "/excel", method = RequestMethod.GET)
    public void excel(HttpServletResponse response,
                      @ModelAttribute BehalfOrderReq behalfOrderReq, @ModelAttribute Pager pager) throws Exception {

        pager.setPage(1);
        pager.setSize(Integer.MAX_VALUE);
        behalfOrderReq.setOrderNo(null);
        behalfOrderReq.setOutOrderNo(null);
        behalfOrderReq.setDealAccount(null);
        behalfOrderReq.setLastId(null);
        if(StringUtils.isEmpty(behalfOrderReq.getBeginTime())||StringUtils.isEmpty(behalfOrderReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,null,null, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("订单号");
        titles.add("商户账号");
        titles.add("订单金额");
        titles.add("卡商账号");
        titles.add("下单时间");
        titles.add("成功时间");
        titles.add("结束时间");
        titles.add("订单状态");
        titles.add("处理人");
        titles.add("备注");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (BehalfOrderVo behalfOrderVo : listByCondition.getContent()) {
            List<Object> row = new ArrayList();
            row.add(behalfOrderVo.getOrderNo());
            row.add(behalfOrderVo.getWithdrawAccount());
            row.add(behalfOrderVo.getAmount());
            row.add(behalfOrderVo.getBehalfAccount());
            row.add(behalfOrderVo.getCreateTime());
            row.add(behalfOrderVo.getSuccessTime());
            row.add(behalfOrderVo.getCloseTime());
            if("PROCESS".equals(behalfOrderVo.getStatus())){
                row.add("处理中");
            }else if("SUCCESS".equals(behalfOrderVo.getStatus())){
                row.add("付款成功");
            }else if("FAIL".equals(behalfOrderVo.getStatus())){
                row.add("付款失败");
            }else{
                row.add("未知");
            }
            row.add(behalfOrderVo.getDealAccount());
            row.add(behalfOrderVo.getRemark());
            rows.add(row);
        }
        data.setRows(rows);
        ExportExcelUtils.exportExcel(response,"导出代付订单列表.xlsx",data);
    }
}


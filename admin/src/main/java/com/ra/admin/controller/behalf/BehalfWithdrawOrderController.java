package com.ra.admin.controller.behalf;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.base.ApiResult;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.entity.business.BehalfOrder;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.BehalfOrderReq;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.BehalfOrderTotalVo;
import com.ra.service.bean.resp.BehalfOrderVo;
import com.ra.service.bean.resp.WalletLogVo;
import com.ra.service.business.BehalfOrderService;
import com.ra.service.component.UploadComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/private/behalfWithdrawOrder")
public class BehalfWithdrawOrderController {

    @Autowired
    BehalfOrderService behalfOrderService;
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
        Long behalfUserId=TokenUtil.getLoginId();
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,null,behalfUserId, pager);
        long lastId=0l;
        if(listByCondition.getContent().size()>0){
            lastId=listByCondition.getContent().get(0).getId();
        }
        for(BehalfOrderVo behalfOrderVo:listByCondition){
            BankCard bankCard= JSON.parseObject(behalfOrderVo.getBankCardInfoJson(), BankCard.class);
            behalfOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());
            behalfOrderVo.setRealName(bankCard.getRealName());//持卡人姓名
            behalfOrderVo.setBankNo(bankCard.getBankNo());//收款卡号
            BehalfBankCard behalfBankCard= JSON.parseObject(behalfOrderVo.getBehalfBankCardInfoJson(), BehalfBankCard.class);
            behalfOrderVo.setBehalfBankCardInfo(behalfBankCard.getRealName()+'：'+behalfBankCard.getBankName()+'：'+behalfBankCard.getBankNo()+'：'+behalfBankCard.getBankBranch());
        }
        String url= UploadComponent.getPayHost(request);
        BehalfOrderTotalVo behalfOrderTotal=behalfOrderService.findByBehalfOrderTotal(behalfOrderReq,null,behalfUserId);
        model.addAttribute("listByCondition", listByCondition).addAttribute("behalfOrderReq", behalfOrderReq)
               .addAttribute("payUrl",url).addAttribute("pager", pager).addAttribute("behalfOrderTotal", behalfOrderTotal).addAttribute("lastId",lastId);
        return "business/behalf/behalfOrder/list";

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
            if(user.getAgencyType()!= AgencyEnum.BEHALF){
                throw new IllegalArgumentException("只有卡商才有权限处理!");
            }
            remark="卡商:"+user.getAccount()+"确认下发,已转账";
            boolean b = behalfOrderService.confirmOrder(orderId, paymentVoucher, remark);
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
            BehalfOrder behalfOrder=behalfOrderService.getRepository().findBehalfOrderById(id);
            Assert.notNull(behalfOrder,"查询不到该提现订单");
           /* Assert.notNull(behalfOrder.getPaymentVoucher(),"请联系卡商上传凭证，不然无法确认订单");*/
            if(user.getAgencyType()!= AgencyEnum.BEHALF&&behalfOrder.getBehalfUserId()==user.getId()){
                throw new IllegalArgumentException("只有该代付订单的卡商才有权限确认该订单!");
            }
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
     * 弹出取消订单
     * @param model
     * @return
     */
    @GetMapping("/cancel")
    public String updateMoney(Model model,@RequestParam("id") Long id){
        try{
            model.addAttribute("id",id);
            return "business/behalf/behalfOrder/cancelRemark";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/behalfWithdrawOrder/findListBehalfOrder";
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
            BehalfOrder behalfOrder=behalfOrderService.getRepository().findBehalfOrderById(id);
            Assert.notNull(behalfOrder,"查询不到该提现订单");
            User user= TokenUtil.getLoginInfo();
            if(user.getAgencyType()!= AgencyEnum.BEHALF&&user.getId()!=behalfOrder.getBehalfUserId()){
                throw new IllegalArgumentException("只有该订单的卡商才有权限取消!");
            }
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
     * 检查新订单
     * @param behalfOrderReq lastId 页面最大的订单id
     * @return
     */
    @PostMapping("/newOrder")
    @ResponseBody
    public ApiResult newOrder(@ModelAttribute BehalfOrderReq behalfOrderReq){
        ApiResult result=new ApiResult();
        try{
            Long behalfUserId=TokenUtil.getLoginId();
            Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,null,behalfUserId, new Pager());
            if(listByCondition.getContent().size()>0){
                return result.ok();
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
        behalfOrderReq.setDealAccount(null);
        behalfOrderReq.setOrderNo(null);
        behalfOrderReq.setOutOrderNo(null);
        if(StringUtils.isEmpty(behalfOrderReq.getBeginTime())||StringUtils.isEmpty(behalfOrderReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        long behalfUserId=TokenUtil.getLoginId();
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,null,behalfUserId, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("订单号");
        titles.add("商户账号");
        titles.add("收款信息");
        titles.add("提现金额");
        titles.add("提现时间");
        titles.add("回调时间");
        titles.add("结束时间");
        titles.add("付款信息");
        titles.add("状态");
        titles.add("处理人");
        titles.add("备注");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (BehalfOrderVo behalfOrderVo:listByCondition) {
            BankCard bankCard= JSON.parseObject(behalfOrderVo.getBankCardInfoJson(), BankCard.class);
            behalfOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());

            BehalfBankCard behalfBankCard= JSON.parseObject(behalfOrderVo.getBehalfBankCardInfoJson(), BehalfBankCard.class);
            behalfOrderVo.setBehalfBankCardInfo(behalfBankCard.getRealName()+'：'+behalfBankCard.getBankName()+'：'+behalfBankCard.getBankNo()+'：'+behalfBankCard.getBankBranch());
            List<Object> row = new ArrayList();
            row.add(behalfOrderVo.getOrderNo());
            row.add(behalfOrderVo.getWithdrawAccount());
            row.add(behalfOrderVo.getBankCardInfo());
            row.add(behalfOrderVo.getAmount());
            row.add(behalfOrderVo.getCreateTime());
            row.add(behalfOrderVo.getCompleteTime());
            row.add(behalfOrderVo.getCloseTime());
            row.add(behalfOrderVo.getBehalfBankCardInfo());
            if("PROCESS".equals(behalfOrderVo.getStatus())){
                row.add("处理中");
            }else if("SUCCESS".equals(behalfOrderVo.getStatus())){
                row.add("成功");
            }else{
                row.add("失败");
            }
            row.add(behalfOrderVo.getDealAccount());
            row.add(behalfOrderVo.getRemark());
            rows.add(row);
        }
        data.setRows(rows);
        //生成本地
        /*File f = new File("c:/test.xlsx");
        FileOutputStream out = new FileOutputStream(f);
        ExportExcelUtils.exportExcel(data, out);
        out.close();*/
        ExportExcelUtils.exportExcel(response,"导出代付订单列表.xlsx",data);
    }
}


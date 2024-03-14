package com.ra.admin.controller.admin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.admin.utils.TokenUtil;
import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.base.ApiResult;
import com.ra.common.bean.ExtraData;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.resp.BehalfBankCardResp;
import com.ra.service.bean.resp.PayOrderTotalVo;
import com.ra.service.bean.resp.PayOrderVo;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayCodeService;
import com.ra.service.business.PayOrderService;
import com.ra.service.component.PayOrderComponent;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/private/payOrder")
public class PayOrderController {
    @Autowired
    PayOrderService payOrderService;
    @Autowired
    PayChannelService payChannelService;
    @Autowired
    PayCodeService payCodeService;
    @Autowired
    MerchantInfoService merchantInfoService;
    @Autowired
    private PayOrderComponent payOrderComponent;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/findListPayOrder", method = RequestMethod.GET)
    public String findListPayOrder(@ModelAttribute PayOrderReq payOrderReq, Model model, @ModelAttribute Pager pager){
//        pager.setSize(20);
        Page<PayOrderVo> listByCondition = payOrderService.findListByPayOrder(payOrderReq, pager);

        List<PayCode>paycodeList=payCodeService.getRepository().findAll();
        for (PayOrderVo payOrderVo : listByCondition) {
            PayChannel payChannel = JSON.parseObject(payOrderVo.getPayChannelInfoJson(), PayChannel.class);
            payOrderVo.setPayChannelName(payChannel.getChannelName());

            for (PayCode payCode : paycodeList) {
                if(payOrderVo.getPayCode().equals(payCode.getCode())){
                    payOrderVo.setPayCode(payCode.getName());
                    break;
                }
            }
        }
        PayOrderTotalVo otcTotalVo=payOrderService.findByPayOrderTotal(payOrderReq);
        BigDecimal successRate;
        if(listByCondition!=null){
            if(otcTotalVo!=null&&otcTotalVo.getOtcSuccessCount().compareTo(BigDecimal.ZERO)==0||listByCondition.getTotalElements()==0){
                successRate=BigDecimal.ZERO;
            }else{
                BigDecimal totalElements = BigDecimal.valueOf(listByCondition.getTotalElements());
                successRate=otcTotalVo.getOtcSuccessCount().divide(totalElements,4,BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        }else{
            successRate=BigDecimal.ZERO;
        }


        /*OtcOrderTotalVo otcOrderTotalVo=null;
        Object obj = redisComponent.get(RedisAdminConstant.get(otcOrderReq));
        if(obj!=null){
            otcOrderTotalVo=(OtcOrderTotalVo)obj;
        }else{
            otcOrderTotalVo=otcOrderService.findByOtcOrderTotal(otcOrderReq);
            redisComponent.set(RedisAdminConstant.getOtcOrderTotalKey(otcOrderReq),otcOrderTotalVo,60*5);
        }*/

        List<User> allProxyList = userService.findAllProxyList();
        List<PayChannel> payChannelList = payChannelService.getRepository().findAll();
        model.addAttribute("listByCondition", listByCondition).addAttribute("paycodeList", paycodeList).addAttribute("payChannelList", payChannelList).addAttribute("allProxyList",allProxyList)
                .addAttribute("otcTotalVo",otcTotalVo).addAttribute("payOrderReq", payOrderReq).addAttribute("pager", pager).addAttribute("successRate",successRate);
        return "business/admin/payOrder/list";
    }

    @PostMapping("/fixOrder")
    @ResponseBody
    public ApiResult fixOrder(@RequestParam String orderNo){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(orderNo,"订单ID不能为空");
            PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
            Assert.notNull(payOrder,"订单不存在");

            if(payOrder.getStatus()== OrderStatusEnum.SUCCESS){
                Assert.isTrue(payOrder.getCompleteTime()==null,"请不要重复回调");
                //如果是成功订单，就补回调
                payOrderComponent.notifyOrder(payOrder,TokenUtil.getLoginInfo().getAccount()+"手动补回调");
                return result.ok("操作成功");
            }else{
                //如果是处理中或者失败，就补单
                boolean b=payOrderService.successOrder(payOrder,TokenUtil.getLoginInfo().getAccount()+"手动补单,等待回调");//更新为成功订单
                if(b){
                    return result.ok("补单成功");
                }
            }
            return result.fail("补单失败");
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }
    @GetMapping("/income")
    public String income(Model model, @RequestParam String orderNo){
        Assert.notNull(orderNo,"订单ID不能为空");
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
        Assert.notNull(payOrder,"订单不存在");

        Rate rate=JSON.parseObject(payOrder.getRateInfoJson(), Rate.class);
        PayChannel payChannel=JSON.parseObject(payOrder.getPayChannelInfoJson(),PayChannel.class);

        MerchantInfo merchantInfo = merchantInfoService.findByUserId(payOrder.getMerchantUserId());
        if(merchantInfo.getProxyUserId()==null||merchantInfo.getProxyUserId()<=0){
            //如果不存在代理，就在计算的时候把代理费率设置成商户费率一样的，这样就会计算出代理费用0元
            rate.setProxyRate(rate.getMerchantRate());
        }
        model.addAttribute("rate",rate);
        model.addAttribute("payOrder",payOrder);
        model.addAttribute("payChannel",payChannel);
        return "business/admin/payOrder/income";
    }


    @GetMapping("/behalfBankCardInfo")
    public String behalfBankCardInfo(Model model, @RequestParam String orderNo){
        Assert.notNull(orderNo,"订单ID不能为空");
        PayOrder payOrder = payOrderService.getRepository().findByOrderNo(orderNo);
        Assert.notNull(payOrder,"订单不存在");
        if(!StringUtils.isEmpty(payOrder.getExtraData())){
            ExtraData extraData=JSON.parseObject(payOrder.getExtraData(),ExtraData.class);
            BehalfBankCardResp bankCardResp=JSON.parseObject(extraData.getData().toString(), BehalfBankCardResp.class);
            User user=userService.getRepository().findUserById( bankCardResp.getBehalfUserId());
            model.addAttribute("bankCardResp",bankCardResp);
            model.addAttribute("user",user);
            return "business/admin/payOrder/behalfBankCardInfo";
        }else{
            model.addAttribute("message","数据异常");
            return "error";
        }
    }
    /**
     * 导出接单订单列表
     * @param response
     * @param payOrderReq
     * @param pager
     * @throws Exception
     */
    @RequestMapping(value = "/excel", method = RequestMethod.GET)
    public void excel(HttpServletResponse response,
                      @ModelAttribute PayOrderReq payOrderReq, @ModelAttribute Pager pager) throws Exception {

        pager.setPage(1);
        pager.setSize(Integer.MAX_VALUE);
        payOrderReq.setOrderNo(null);
        payOrderReq.setAmount(null);
        payOrderReq.setMerchantAccount(null);
        payOrderReq.setOutOrderNo(null);
        payOrderReq.setPayChannelId(null);
        payOrderReq.setPayCode(null);
        if(StringUtils.isEmpty(payOrderReq.getBeginTime())||StringUtils.isEmpty(payOrderReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        Page<PayOrderVo> listByCondition = payOrderService.findListByPayOrder(payOrderReq, pager);

        List<PayCode>paycodeList=payCodeService.getRepository().findAll();
        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("订单号");
        titles.add("商户订单号");
        titles.add("商户账号");
        titles.add("订单金额");
        titles.add("支付编码");
        titles.add("通道名称");
        titles.add("下单时间");
        titles.add("回调时间");
        titles.add("结束时间");
        titles.add("订单状态");
        titles.add("状态描述");
        titles.add("第三方异常");
        data.setTitles(titles);
        PayChannel payChannel=new PayChannel();
        List<List<Object>> rows = new ArrayList();
        for (PayOrderVo payOrderVo : listByCondition.getContent()) {
             payChannel = JSON.parseObject(payOrderVo.getPayChannelInfoJson(), PayChannel.class);
            payOrderVo.setPayChannelName(payChannel.getChannelName());
            for (PayCode payCode : paycodeList) {
                if(payOrderVo.getPayCode().equals(payCode.getCode())){
                    payOrderVo.setPayCode(payCode.getName());
                    break;
                }
            }
            List<Object> row = new ArrayList();
            row.add(payOrderVo.getOrderNo());
            row.add(payOrderVo.getOutOrderNo());
            row.add(payOrderVo.getMerchantAccount());
            row.add(payOrderVo.getAmount());
            row.add(payOrderVo.getPayCode());
            row.add(payOrderVo.getPayChannelName());
            row.add(payOrderVo.getCreateTime());
            row.add(payOrderVo.getCompleteTime());
            row.add(payOrderVo.getCloseTime());
            if("PROCESS".equals(payOrderVo.getStatus())){
                row.add("处理中");
            }else if("SUCCESS".equals(payOrderVo.getStatus())){
                row.add("支付成功");
            }else if("FAIL".equals(payOrderVo.getStatus())){
                row.add("支付失败");
            }else{
                row.add("未知");
            }
            row.add(payOrderVo.getStatusDesc());
            row.add(payOrderVo.getErrorMsg());
            rows.add(row);
        }
        data.setRows(rows);
        //生成本地
        /*File f = new File("c:/test.xlsx");
        FileOutputStream out = new FileOutputStream(f);
        ExportExcelUtils.exportExcel(data, out);
        out.close();*/
        ExportExcelUtils.exportExcel(response,"导出接单订单列表.xlsx",data);
    }
}

package com.ra.admin.controller.proxy;

import com.alibaba.fastjson.JSON;
import com.ra.admin.utils.TokenUtil;
import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.resp.PayOrderVo;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayCodeService;
import com.ra.service.business.PayOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/private/proxyPayOrder")
public class MyMerchantOrderController {
    @Autowired
    PayOrderService payOrderService;

   /* @Autowired
    PayChannelService payChannelService;*/
    @Autowired
    PayCodeService payCodeService;

    @RequestMapping(value = "/findListProxyPayOrder", method = RequestMethod.GET)
    public String findListProxyPayOrder(@ModelAttribute PayOrderReq payOrderReq, Model model, @ModelAttribute Pager pager){
        pager.setSize(20);
        long proxyUserId=TokenUtil.getLoginId();
        Page<PayOrderVo> listByCondition = payOrderService.findListByProxyPayOrder(payOrderReq, proxyUserId,pager);
        List<PayCode> paycodeList=payCodeService.getRepository().findAll();
        for (PayOrderVo payOrderVo : listByCondition) {
            for (PayCode payCode : paycodeList) {
                if(payOrderVo.getPayCode().equals(payCode.getCode())){
                    payOrderVo.setPayCode(payCode.getName());
                    break;
                }
            }
        }

       /* List<PayChannel> payChannelList = payChannelService.getRepository().findAll();*/
        BigDecimal proxyMerchantOrderTotal=payOrderService.findByProxyPayOrderTotal(payOrderReq,proxyUserId);
        model.addAttribute("listByCondition", listByCondition).addAttribute("paycodeList", paycodeList)
                .addAttribute("payOrderReq", payOrderReq)
                .addAttribute("pager", pager).addAttribute("proxyMerchantOrderTotal",proxyMerchantOrderTotal);
        return "business/proxy/payOrder/list";
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
        long proxyUserId=TokenUtil.getLoginId();
        Page<PayOrderVo> listByCondition = payOrderService.findListByProxyPayOrder(payOrderReq, proxyUserId,pager);

        List<PayCode>paycodeList=payCodeService.getRepository().findAll();
        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("订单号");
        titles.add("商户订单号");
        titles.add("商户账号");
        titles.add("订单金额");
        titles.add("支付编码");
        titles.add("订单时间");
        titles.add("回调时间");
        titles.add("结束时间");
        titles.add("订单状态");
        titles.add("状态描述");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (PayOrderVo payOrderVo : listByCondition.getContent()) {
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

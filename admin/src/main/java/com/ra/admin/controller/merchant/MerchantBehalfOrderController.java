package com.ra.admin.controller.merchant;

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
import com.ra.service.bean.resp.BehalfOrderTotalVo;
import com.ra.service.bean.resp.BehalfOrderVo;
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
@RequestMapping("/private/merchantBehalfOrder")
public class MerchantBehalfOrderController {

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
         Long merchantUserId=TokenUtil.getLoginId();
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,merchantUserId,null, pager);
        if(listByCondition!=null){
            for(BehalfOrderVo behalfOrderVo:listByCondition){
                BankCard bankCard= JSON.parseObject(behalfOrderVo.getBankCardInfoJson(), BankCard.class);
                behalfOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());
            }
        }
        String url= UploadComponent.getPayHost(request);
        BehalfOrderTotalVo merchantOrderTotal=behalfOrderService.findByBehalfOrderTotal(behalfOrderReq,merchantUserId,null);
        model.addAttribute("listByCondition", listByCondition).addAttribute("behalfOrderReq", behalfOrderReq)
               .addAttribute("payUrl",url).addAttribute("pager", pager).addAttribute("merchantOrderTotal",merchantOrderTotal);
        return "business/merchant/behalfOrder/list";

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
        behalfOrderReq.setWithdrawAccount(null);
        if(StringUtils.isEmpty(behalfOrderReq.getBeginTime())||StringUtils.isEmpty(behalfOrderReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        long merchantUserId=TokenUtil.getLoginId();
        Page<BehalfOrderVo> listByCondition = behalfOrderService.findBehalfOrderList(behalfOrderReq,merchantUserId,null, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("订单号");
        titles.add("收款信息");
        titles.add("提现金额");
        titles.add("提现时间");
        titles.add("回调时间");
        titles.add("结束时间");
        titles.add("状态");
        titles.add("备注");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (BehalfOrderVo behalfOrderVo:listByCondition) {
            BankCard bankCard= JSON.parseObject(behalfOrderVo.getBankCardInfoJson(), BankCard.class);
            behalfOrderVo.setBankCardInfo(bankCard.getRealName()+'：'+bankCard.getBankName()+'：'+bankCard.getBankNo()+'：'+bankCard.getBankBranch());

            List<Object> row = new ArrayList();
            row.add(behalfOrderVo.getOutOrderNo());
            row.add(behalfOrderVo.getBankCardInfo());
            row.add(behalfOrderVo.getAmount());
            row.add(behalfOrderVo.getCreateTime());
            row.add(behalfOrderVo.getCompleteTime());
            row.add(behalfOrderVo.getCloseTime());
            if("PROCESS".equals(behalfOrderVo.getStatus())){
                row.add("处理中");
            }else if("SUCCESS".equals(behalfOrderVo.getStatus())){
                row.add("成功");
            }else{
                row.add("失败");
            }
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


package com.ra.admin.controller.admin;

import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.domain.Pager;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.BehalfProfitLogVo;
import com.ra.service.business.BehalfProfitLogService;
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
@RequestMapping("/private/profitLog")
public class BehalfProfitLogController {
    @Autowired
    BehalfProfitLogService behalfProfitLogService;

    /**
     * 卡商的利润变动列表
     * @param walletLogReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListProfitLog", method = RequestMethod.GET)
    public String findListWalletLog(@ModelAttribute WalletLogReq walletLogReq, Model model, @ModelAttribute Pager pager){
        pager.setSize(20);
        Page<BehalfProfitLogVo> listByCondition = behalfProfitLogService.findBehalfProfitLogList(walletLogReq, pager);
        BigDecimal profitLogTotal=behalfProfitLogService.findByBehalfProfitTotal(walletLogReq);
        model.addAttribute("listByCondition", listByCondition).addAttribute("walletLogReq", walletLogReq)
                .addAttribute("profitLogTotal",profitLogTotal)
                .addAttribute("pager", pager);
        return "business/admin/walletLog/profitLogList";

    }

    /**
     * 导出卡商利润变动日志
     * @param response
     * @param walletLogReq
     * @param pager
     * @throws Exception
     */
    @RequestMapping(value = "/excel", method = RequestMethod.GET)
    public void excel(HttpServletResponse response,
                      @ModelAttribute WalletLogReq walletLogReq, @ModelAttribute Pager pager) throws Exception {

        pager.setPage(1);
        pager.setSize(Integer.MAX_VALUE);
        walletLogReq.setLogEnum(null);
        walletLogReq.setQueryParam(null);
        walletLogReq.setWay(null);
        if(StringUtils.isEmpty(walletLogReq.getBeginTime())||StringUtils.isEmpty(walletLogReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        Page<BehalfProfitLogVo> listByCondition = behalfProfitLogService.findBehalfProfitLogList(walletLogReq, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("账号");
        titles.add("变动前余额");
        titles.add("变动金额");
        titles.add("变动后余额");
        titles.add("变动时间");
        titles.add("变动方式");
        titles.add("描述");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (BehalfProfitLogVo behalfProfitLogVo : listByCondition.getContent()) {
            List<Object> row = new ArrayList();
            row.add(behalfProfitLogVo.getAccount());
            row.add(behalfProfitLogVo.getBeforeBalance());
            row.add(behalfProfitLogVo.getAmount());
            row.add(behalfProfitLogVo.getBalance());
            row.add(behalfProfitLogVo.getCreateTime());
            if("rollIn".equals(behalfProfitLogVo.getWay())){
                row.add("转入");
            }else if("rollOut".equals(behalfProfitLogVo.getWay())){
                row.add("转出");
            }else{
                row.add("未知");
            }
            row.add(behalfProfitLogVo.getDescription());
            rows.add(row);
        }
        data.setRows(rows);
        ExportExcelUtils.exportExcel(response,"导出卡商利润变动日志列表.xlsx",data);
    }
}

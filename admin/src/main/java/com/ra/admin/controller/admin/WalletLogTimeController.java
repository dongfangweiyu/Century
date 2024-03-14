package com.ra.admin.controller.admin;

import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.WalletLogTimeVo;
import com.ra.service.bean.resp.WalletLogVo;
import com.ra.service.business.WalletLogService;
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

/**
 * 根据某个时间点查瞬时的最后金额
 */
@Controller
@RequestMapping("/private/walletLogTime")
public class WalletLogTimeController {
    @Autowired
    WalletLogService walletLogService;

    /**
     * 根据某个时间点查瞬时的最后金额列表
     * @param walletLogReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListWalletLog", method = RequestMethod.GET)
    public String findListWalletLog(@ModelAttribute WalletLogReq walletLogReq, Model model, @ModelAttribute Pager pager){
        pager.setSize(20);
        Page<WalletLogTimeVo> listByCondition = walletLogService.findWalletLogTimeList(walletLogReq, pager);
        BigDecimal total=walletLogService.findWalletLogTimeTotal(walletLogReq);
        model.addAttribute("listByCondition", listByCondition).
                addAttribute("total",total).addAttribute("walletLogReq", walletLogReq)
                .addAttribute("pager", pager).addAttribute("walletLogEnum", WalletLogEnum.values());
        return "business/admin/walletLog/listTime";

    }

    /**
     * 导出根据某个时间点查瞬时的最后金额
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

        Page<WalletLogTimeVo> listByCondition = walletLogService.findWalletLogTimeList(walletLogReq, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("账号");
        titles.add("瞬时余额");
        titles.add("时间");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (WalletLogTimeVo walletLogVo : listByCondition.getContent()) {
            List<Object> row = new ArrayList();
            row.add(walletLogVo.getAccount());
            row.add(walletLogVo.getBalance());
            row.add(walletLogVo.getCreateTime());
            rows.add(row);
        }
        data.setRows(rows);
        //生成本地
        /*File f = new File("c:/test.xlsx");
        FileOutputStream out = new FileOutputStream(f);
        ExportExcelUtils.exportExcel(data, out);
        out.close();*/
        ExportExcelUtils.exportExcel(response,"导出卡点查账列表.xlsx",data);
    }
}

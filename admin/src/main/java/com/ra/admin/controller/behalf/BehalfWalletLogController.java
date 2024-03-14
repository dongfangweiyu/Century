package com.ra.admin.controller.behalf;

import com.ra.admin.utils.TokenUtil;
import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.service.bean.req.WalletLogReq;
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/private/behalfWalletLog")
public class BehalfWalletLogController {
    @Autowired
    WalletLogService walletLogService;

    /**
     * 代付商的金额变动列表
     * @param walletLogReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListWalletLog", method = RequestMethod.GET)
    public String findListWalletLog(@ModelAttribute WalletLogReq walletLogReq, Model model, @ModelAttribute Pager pager){
        long behalfUserId=TokenUtil.getLoginId();
        Page<WalletLogVo> listByCondition = walletLogService.findWalletLogList(walletLogReq, behalfUserId, pager);
        model.addAttribute("listByCondition", listByCondition).addAttribute("walletLogReq", walletLogReq)
                .addAttribute("pager", pager).addAttribute("walletLogEnum", WalletLogEnum.values());
        return "business/behalf/walletLog/list";

    }
    /**
     * 导出金额变动日志
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
        long behalfUserId=TokenUtil.getLoginId();
        Page<WalletLogVo> listByCondition = walletLogService.findWalletLogList(walletLogReq, behalfUserId, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("账号");
        titles.add("变动前余额");
        titles.add("变动金额");
        titles.add("变动后余额");
        titles.add("变动时间");
        titles.add("变动方式");
        titles.add("变动类型");
        titles.add("描述");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (WalletLogVo walletLogVo : listByCondition.getContent()) {
            List<Object> row = new ArrayList();
            row.add(walletLogVo.getAccount());
            row.add(walletLogVo.getBeforeBalance());
            row.add(walletLogVo.getAmount());
            row.add(walletLogVo.getBalance());
            row.add(walletLogVo.getCreateTime());
            if("rollIn".equals(walletLogVo.getWay())){
                row.add("转入");
            }else if("rollOut".equals(walletLogVo.getWay())){
                row.add("转出");
            }else{
                row.add("未知");
            }
            row.add(walletLogVo.getLogEnum().getText());
            row.add(walletLogVo.getDescription());
            rows.add(row);
        }
        data.setRows(rows);
        //生成本地
        /*File f = new File("c:/test.xlsx");
        FileOutputStream out = new FileOutputStream(f);
        ExportExcelUtils.exportExcel(data, out);
        out.close();*/
        ExportExcelUtils.exportExcel(response,"导出金额变动日志列表.xlsx",data);
    }
}

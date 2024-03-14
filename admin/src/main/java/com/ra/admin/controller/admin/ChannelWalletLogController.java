package com.ra.admin.controller.admin;

import com.ra.admin.utils.excel.ExcelData;
import com.ra.admin.utils.excel.ExportExcelUtils;
import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.ChannelWalletLog;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.service.bean.req.ChannelWalletLogReq;
import com.ra.service.bean.resp.ChannelWalletLogVo;
import com.ra.service.business.ChannelWalletLogService;
import com.ra.service.business.ConfigPayInterfaceService;
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

/**
 * 通道余额变动日志
 */
@Controller
@RequestMapping("/private/channelWalletLog")
public class ChannelWalletLogController {

    @Autowired
    private ConfigPayInterfaceService configPayInterfaceService;
    @Autowired
    ChannelWalletLogService channelWalletLogService;

    /**
     * 通道金额变动列表
     * @param channelWalletLogReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String findListWalletLog(@ModelAttribute ChannelWalletLogReq channelWalletLogReq, Model model, @ModelAttribute Pager pager){
        pager.setSize(20);

        List<ConfigPayInterface> configPayInterfaceList = configPayInterfaceService.getRepository().findAll();

        Page<ChannelWalletLogVo> listByCondition = channelWalletLogService.findChannelWalletLogList(channelWalletLogReq, pager);

        model.addAttribute("listByCondition", listByCondition).addAttribute("walletLogReq", channelWalletLogReq)
                .addAttribute("pager", pager).addAttribute("configPayInterfaceList",configPayInterfaceList);
        return "business/admin/walletLog/channelWalletList";

    }

    /**
     * 导出通道金额变动日志
     * @param response
     * @param channelWalletLogReq
     * @param pager
     * @throws Exception
     */
    @RequestMapping(value = "/excel", method = RequestMethod.GET)
    public void excel(HttpServletResponse response,
                      @ModelAttribute ChannelWalletLogReq channelWalletLogReq, @ModelAttribute Pager pager) throws Exception {

        pager.setPage(1);
        pager.setSize(Integer.MAX_VALUE);
        if(StringUtils.isEmpty(channelWalletLogReq.getBeginTime())||StringUtils.isEmpty(channelWalletLogReq.getEndTime())){
            throw new IllegalArgumentException("请选择时间导出");
        }
        Page<ChannelWalletLogVo> listByCondition = channelWalletLogService.findChannelWalletLogList(channelWalletLogReq, pager);

        ExcelData data = new ExcelData();
        data.setName(null);
        List<String> titles = new ArrayList();
        titles.add("通道配置名称(配置ID)");
        titles.add("变动前余额");
        titles.add("变动金额");
        titles.add("变动后余额");
        titles.add("变动时间");
        titles.add("描述");
        data.setTitles(titles);
        List<List<Object>> rows = new ArrayList();
        for (ChannelWalletLogVo walletLogVo : listByCondition.getContent()) {
            List<Object> row = new ArrayList();
            row.add(walletLogVo.getConfigName()+"("+walletLogVo.getConfigPayInterfaceId()+")");
            row.add(walletLogVo.getBeforeBalance());
            row.add(walletLogVo.getAmount());
            row.add(walletLogVo.getBalance());
            row.add(walletLogVo.getCreateTime());
            row.add(walletLogVo.getDescription());
            rows.add(row);
        }
        data.setRows(rows);
        //生成本地
        /*File f = new File("c:/test.xlsx");
        FileOutputStream out = new FileOutputStream(f);
        ExportExcelUtils.exportExcel(data, out);
        out.close();*/
        ExportExcelUtils.exportExcel(response,"导出通道金额变动日志列表.xlsx",data);
    }
}

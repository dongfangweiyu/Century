package com.ra.admin.securitys;

import com.ra.common.base.ApiResult;
import com.ra.dao.entity.security.Page;
import com.ra.service.security.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-17
 * @description
 */
@Controller
@RequestMapping("/private/page/")
public class PageController {


    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private PageService pageService;

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public String list(Model model, @ModelAttribute Page path) {
        List<Page> listPage = pageService.findListPage(null);
        model.addAttribute("page", listPage).addAttribute("params", path);
        return "security/page/list";
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("page", new Page()).addAttribute("_method", "PUT");
        return "security/page/form";
    }

    @RequestMapping(value = "/create", method = RequestMethod.PUT)
    public String create(@ModelAttribute Page page, Model model) {
        try {
            pageService.getRepository().save(page);
            return "redirect:/private/page/list";
        } catch (Exception e) {
            logger.error("[添加路径异常]", e);
            model.addAttribute("page", page).addAttribute("msm", "提交异常");
            return "security/page/form";
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(Model model, @RequestParam("id") String id) {
        Page page = pageService.getRepository().findPageById(Long.valueOf(id));
        model.addAttribute("page", page).addAttribute("_method", "PUT");
        return "security/page/form";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT)
    public String edit(@ModelAttribute Page page) {
        pageService.getRepository().save(page);
        return "redirect:/private/page/list";
    }

    @RequestMapping(value = "/remove", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult remove(@RequestParam("id") Long id) {
        ApiResult apiResult = new ApiResult();
        try {
            Page page = pageService.getRepository().findPageById(id);
            Assert.notNull(page,"页面不存在");
            pageService.getRepository().delete(page);
            apiResult.ok();
        } catch (Exception e) {
            logger.error("[路径删除异常]", e);
            apiResult.fail("删除失败");
        }
        return apiResult;
    }

}

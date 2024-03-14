package com.ra.admin.controller.admin;

import com.ra.common.base.ApiResult;
import com.ra.common.bean.LayUIData;
import com.ra.common.utils.IpUtil;
import com.ra.dao.channel.PayInterfaceEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.business.PayChannelService;
import com.ra.service.business.PayCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付通道管理
 */
@Controller
@RequestMapping("/private/payChannel")
public class PayChannelController {

    @Autowired
    private PayChannelService payChannelService;
    @Autowired
    private PayCodeService payCodeService;
    @Autowired
    private ConfigPayInterfaceRepository configPayInterfaceRepository;

    @GetMapping("/list")
    public String list(Model model){

        List<PayChannel> all = payChannelService.getRepository().findAll();
        for (PayChannel payChannel : all) {
            payChannel.setRate(payChannel.getRate().multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP));
        }

        model.addAttribute("list",all);
        return "business/admin/payChannel/list";
    }

    @GetMapping("/add")
    public String add(Model model){
        List<PayCode> all = payCodeService.getRepository().findAll();
        List<LayUIData> layUIData = all.stream().map(item -> {
            LayUIData bean = new LayUIData();
            bean.setTitle(item.getName());
            bean.setValue(item.getId().toString());
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("bean",new PayChannel());
        model.addAttribute("transferData", layUIData);
        model.addAttribute("selectedPayCodesIds",new ArrayList<>());
        model.addAttribute("payInterfaceValues", PayInterfaceEnum.values());
        model.addAttribute("_method","POST");
        return "business/admin/payChannel/form";
    }

    @PostMapping("/add")
    public String add(Model model, @ModelAttribute PayChannel payChannel,String[] payCodeIds){
        try{
            Assert.hasText(payChannel.getChannelName(),"通道名称不能为空");
            Assert.notNull(payChannel.getPayInterface(),"支付接口不能为空");
            Assert.notNull(payChannel.getRate(),"通道第三方费率不能为空");
            Assert.hasText(payChannel.getChannelHost(),"第三方官网不能为空");
            Assert.isTrue(payChannel.getConfigPayInterfaceId()>0,"请选择接口配置");
//            Assert.notNull(payChannel.getCreditScore(),"信誉分不能为空");

            payChannelService.savePayChannel(payChannel, payCodeIds);
            return "redirect:/private/payChannel/list";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",payChannel);
            model.addAttribute("_method","POST");
        }
        return "business/admin/payChannel/form";
    }

    @GetMapping("/edit")
    public String edit(Model model,@RequestParam long id){

        PayChannel payChannel = payChannelService.getRepository().findById(id);
        Assert.notNull(payChannel,"查询不到该支付通道");

        payChannel.setRate(payChannel.getRate().multiply(BigDecimal.valueOf(100)).setScale(3,BigDecimal.ROUND_UP));

        List<PayCode> all = payCodeService.getRepository().findAll();
        List<LayUIData> layUIData = all.stream().map(item -> {
            LayUIData bean = new LayUIData();
            bean.setTitle(item.getName());
            bean.setValue(item.getId().toString());
            return bean;
        }).collect(Collectors.toList());

        List<PayCode> payCodeByChannel2Id = payCodeService.findPayCodeByChannel2Id(id);
        List<Long> selectedPayCodesIds=payCodeByChannel2Id.stream().map(item->{
            return item.getId();
        }).collect(Collectors.toList());

        List<ConfigPayInterface> configPayInterfaceList = configPayInterfaceRepository.findByPayInterfaceEnum(payChannel.getPayInterface());

        model.addAttribute("bean",payChannel);
        model.addAttribute("transferData", layUIData);
        model.addAttribute("selectedPayCodesIds",selectedPayCodesIds);
        model.addAttribute("payInterfaceValues", PayInterfaceEnum.values());
        model.addAttribute("configPayInterfaceList",configPayInterfaceList);
        model.addAttribute("_method","PUT");
        return "business/admin/payChannel/form";
    }

    @PutMapping("/edit")
    public String edit(Model model, @ModelAttribute PayChannel payChannel,String[] payCodeIds){
        try{
            PayChannel old = payChannelService.getRepository().findById(payChannel.getId().longValue());
            Assert.notNull(old,"查询不到该支付通道");

            Assert.hasText(payChannel.getChannelName(),"通道名称不能为空");
            Assert.notNull(payChannel.getPayInterface(),"支付接口不能为空");
            Assert.hasText(payChannel.getPayInterfaceType(),"支付接口类型不能为空");
            Assert.notNull(payChannel.getRate(),"通道第三方费率不能为空");
            Assert.hasText(payChannel.getChannelHost(),"第三方官网不能为空");
            Assert.isTrue(payChannel.getConfigPayInterfaceId()>0,"请选择接口配置");
//            Assert.notNull(payChannel.getCreditScore(),"信誉分不能为空");

            old.setChannelName(payChannel.getChannelName());
            old.setPayInterface(payChannel.getPayInterface());
            old.setPayInterfaceType(payChannel.getPayInterfaceType());
            old.setRate(payChannel.getRate());
            old.setConfigPayInterfaceId(payChannel.getConfigPayInterfaceId());
            old.setChannelHost(payChannel.getChannelHost());
//            old.setCreditScore(payChannel.getCreditScore());
            payChannelService.savePayChannel(old,payCodeIds);
            return "redirect:/private/payChannel/list";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",payChannel);
            model.addAttribute("_method","PUT");
        }
        return "business/admin/payChannel/form";
    }


    @GetMapping("/risk")
    public String risk(Model model,@RequestParam long channelId){
        try{
            PayChannel old = payChannelService.getRepository().findById(channelId);
            Assert.notNull(old,"查询不到该支付通道");
            model.addAttribute("bean",old);
            return "business/admin/payChannel/risk";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/payChannel/list";
    }

    @PostMapping("/risk")
    @ResponseBody
    public ApiResult risk(Model model,@ModelAttribute PayChannel payChannel){
        ApiResult result=new ApiResult();
        try{
            PayChannel old = payChannelService.getRepository().findById(payChannel.getId().longValue());
            Assert.notNull(old,"查询不到该支付通道");

            old.setRisk(payChannel.isRisk());
            old.setCreditScore(payChannel.getCreditScore());
            old.setBeginTime(payChannel.getBeginTime());
            old.setEndTime(payChannel.getEndTime());
            old.setMinAmount(payChannel.getMinAmount());
            old.setMaxAmount(payChannel.getMaxAmount());

            payChannelService.getRepository().save(old);
            return result.ok();
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }

    @PostMapping("/del")
    @ResponseBody
    public ApiResult del(@RequestParam long id){
        ApiResult result=new ApiResult();
        try{
            PayChannel payChannel = payChannelService.getRepository().findById(id);
            Assert.notNull(payChannel,"查询不到该支付编码");

            payChannelService.getRepository().delete(payChannel);
            return result.ok("删除成功");
        }catch (Exception e){

            return result.fail(e.getMessage());
        }
    }


    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        PayChannel payChannel = payChannelService.getRepository().findById(id);
        Assert.notNull(payChannel,"查询不到该支付编码");

        if(payChannel.isEnable()){
            payChannel.setEnable(false);
        }else {
            payChannel.setEnable(true);
        }
        payChannelService.getRepository().save(payChannel);
        return "redirect:/private/payChannel/list";
    }


    @PostMapping("/getPayInterfaceType")
    @ResponseBody
    public ApiResult getChannelCodeType(@RequestParam PayInterfaceEnum payInterface){
        ApiResult result=new ApiResult();
        try{
            List<ConfigPayInterface> byPayInterfaceEnum = configPayInterfaceRepository.findByPayInterfaceEnum(payInterface);
            return result.ok("获取成功").put("payType",payInterface.getTypes()).put("configList",byPayInterfaceEnum);
        }catch (Exception e){
            return result.fail(e.getMessage());
        }
    }
}

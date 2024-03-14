package com.ra.admin.controller.admin;

import com.alibaba.fastjson.JSONObject;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.channel.PayInterface;
import com.ra.dao.channel.PayInterfaceEnum;
import com.ra.dao.channel.PayInterfaceFactory;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.entity.config.ConfigPayInterface;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.config.ConfigPayInterfaceRepository;
import com.ra.service.business.ConfigPayInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * 支付编码管理
 */
@Controller
@RequestMapping("/private/interface")
public class PayInterfaceController {

    @Autowired
    private ConfigPayInterfaceService configPayInterfaceService;

    @GetMapping("/list")
    public String list(Model model){
        model.addAttribute("list", PayInterfaceEnum.values());
        return "business/admin/payInterface/list";
    }

    @GetMapping("/config")
    public String config(Model model,@RequestParam PayInterfaceEnum interfaceEnum){
        List<ConfigPayInterface> configPayInterfaceList = configPayInterfaceService.getRepository().findByPayInterfaceEnum(interfaceEnum);
//        if(byPayInterfaceEnum==null){
//            byPayInterfaceEnum=new ConfigPayInterface();
//            byPayInterfaceEnum.setPayInterfaceEnum(interfaceEnum);
//        }
        model.addAttribute("list",configPayInterfaceList);
        model.addAttribute("interfaceEnum",interfaceEnum);
        model.addAttribute("_method","POST");
        return "business/admin/payInterface/config";
    }

    @GetMapping("/add")
    public String add(Model model,@RequestParam PayInterfaceEnum interfaceEnum){
        ConfigPayInterface configPayInterface=new ConfigPayInterface();
        configPayInterface.setPayInterfaceEnum(interfaceEnum);

        model.addAttribute("bean",configPayInterface);
        model.addAttribute("_method","POST");
        return "business/admin/payInterface/form";
    }

    @PostMapping("/add")
    @ResponseBody
    public ApiResult add(@ModelAttribute ConfigPayInterface configPayInterface){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(configPayInterface.getConfigName(),"配置名称不能为空");
            Assert.notNull(configPayInterface.getPayInterfaceEnum(),"接口类型不能为空");
            Assert.hasText(configPayInterface.getCreateOrderUrl(),"第三方创建订单接口地址不能为空");
            Assert.hasText(configPayInterface.getAppId(),"第三方商户APPID不能为空");
            Assert.hasText(configPayInterface.getSecret(),"第三方秘钥不能为空");

            boolean url = IpUtil.isURL(configPayInterface.getCreateOrderUrl());
            Assert.isTrue(url,"第三方下单网关地址格式错误");


            String bindIP = configPayInterface.getBindIP();
            if(StringUtils.hasText(bindIP)){
                if(IpUtil.isIp(bindIP)){
                    configPayInterface.setBindIP(bindIP);
                }else{
                    throw new IllegalArgumentException("IP格式不正确");
                }
            }else{
                configPayInterface.setBindIP(null);
            }

            if(!StringUtils.isEmpty(configPayInterface.getJson())){
                try {
                    JSONObject.parseObject(configPayInterface.getJson());
                }catch (Exception e){
                    throw new IllegalArgumentException("自定义参数JSON：格式不合法");
                }
            }
            if(!StringUtils.isEmpty(configPayInterface.getBalanceLimit())){
                if(configPayInterface.getBalanceLimit().compareTo(BigDecimal.ZERO)==-1){
                    throw new IllegalArgumentException("余额限制必须大于0");
                }
            }
            configPayInterfaceService.getRepository().save(configPayInterface);
            return result.ok("配置成功");
        }catch (Exception e){
            System.out.println(e.getMessage());
            return result.fail(e.getMessage());
        }
    }

    @GetMapping("/edit")
    public String edit(Model model,@RequestParam(required = true) long id){
        ConfigPayInterface configPayInterface = configPayInterfaceService.getRepository().findById(id);
        model.addAttribute("bean",configPayInterface);
        model.addAttribute("_method","POST");
        return "business/admin/payInterface/form";
    }

    @PostMapping("/edit")
    @ResponseBody
    public ApiResult edit(@ModelAttribute ConfigPayInterface configPayInterface){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(configPayInterface.getConfigName(),"配置名称不能为空");
            Assert.notNull(configPayInterface.getPayInterfaceEnum(),"接口类型不能为空");
            Assert.hasText(configPayInterface.getCreateOrderUrl(),"第三方创建订单接口地址不能为空");
            Assert.hasText(configPayInterface.getAppId(),"第三方商户APPID不能为空");
            Assert.hasText(configPayInterface.getSecret(),"第三方秘钥不能为空");

            boolean url = IpUtil.isURL(configPayInterface.getCreateOrderUrl());
            Assert.isTrue(url,"第三方下单网关地址格式错误");

            if(!StringUtils.isEmpty(configPayInterface.getJson())){
                try {
                    JSONObject.parseObject(configPayInterface.getJson());
                }catch (Exception e){
                    throw new IllegalArgumentException("自定义参数JSON：格式不合法");
                }
            }

            ConfigPayInterface old = configPayInterfaceService.getRepository().findById(configPayInterface.getId().longValue());
            old.setConfigName(configPayInterface.getConfigName());
            old.setSecret(configPayInterface.getSecret());
            old.setAppId(configPayInterface.getAppId());
            old.setCreateOrderUrl(configPayInterface.getCreateOrderUrl());
            old.setJson(configPayInterface.getJson());
            if(!StringUtils.isEmpty(configPayInterface.getBalanceLimit())){
                if(configPayInterface.getBalanceLimit().compareTo(BigDecimal.ZERO)==-1){
                    throw new IllegalArgumentException("余额限制必须大于0");
                }else{
                    old.setBalanceLimit(configPayInterface.getBalanceLimit());
                }
            }
            String bindIP = configPayInterface.getBindIP();
            if(StringUtils.hasText(bindIP)){
                if(IpUtil.isIp(bindIP)){
                    old.setBindIP(bindIP);
                }else{
                    throw new IllegalArgumentException("IP格式不正确");
                }
            }else{
                old.setBindIP(null);
            }

            configPayInterfaceService.getRepository().save(old);
            return result.ok("修改成功");
        }catch (Exception e){
            System.out.println(e.getMessage());
            return result.fail(e.getMessage());
        }
    }

    /**
     * 弹出修改金额
     * @param model
     * @return
     */
    @GetMapping("/updateMoney")
    public String updateMoney(Model model,@RequestParam("id") Long id){
        try{
            model.addAttribute("id",id);
            return "business/admin/payInterface/updateMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/interface/config";
    }

    /**
     * 修改金额
     * @param id 配置id
     * @param walletMoney 修改的账户余额
     * @return
     */
    @PostMapping("/modifyWalletMoney")
    @ResponseBody
    public ApiResult modifyWalletMoney(@RequestParam("id") Long id,
                                       @RequestParam("walletMoney") BigDecimal walletMoney,
                                       @RequestParam(value = "remark",required = false,defaultValue = "")String remark){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(id, "配置id不能为空");
            Assert.notNull(walletMoney, "修改的金额不能为空");
            Assert.hasText(remark,"备注不能为空");

            User loginInfo = TokenUtil.getLoginInfo();
            String description=remark+"（管理员："+loginInfo.getAccount()+"修改通道金额变动）";
            int i = configPayInterfaceService.addMoney(id, walletMoney,description);
            if(i>0){
                return result.ok();
            }
            return result.fail("修改失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult remove(@RequestParam("id") Long id) {
        ApiResult apiResult = new ApiResult();
        try {
            configPayInterfaceService.getRepository().deleteById(id);
            apiResult.ok();
        } catch (Exception e) {
            apiResult.fail("删除失败");
        }
        return apiResult;
    }

    /**
     * 测试接口
     * @param model
     * @param interfaceEnum
     * @return
     */
    @GetMapping("/test")
    public String test(Model model, @RequestParam PayInterfaceEnum interfaceEnum) {
        List<ConfigPayInterface> configPayInterfaceList = configPayInterfaceService.getRepository().findByPayInterfaceEnum(interfaceEnum);

        model.addAttribute("payInterfaceEnum",interfaceEnum);
        model.addAttribute("types",interfaceEnum.getTypes());
        model.addAttribute("configPayInterfaceList",configPayInterfaceList);
        model.addAttribute("_method","POST");
        return "business/admin/payInterface/test";
    }

    /**
     * 测试接口
     * @param interfaceEnum
     * @return
     */
    @PostMapping("/test")
    public String test(HttpServletRequest request, @RequestParam PayInterfaceEnum interfaceEnum
                       , @RequestParam BigDecimal amount
                       ,@RequestParam String payInterfaceType
                       ,@RequestParam Long configId
                       ) throws Exception {

        PayInterface payInterface = PayInterfaceFactory.newInstance(interfaceEnum);
        ApiResult result = payInterface.onPay(request,PayOrder.test(amount), PayChannel.test(interfaceEnum,payInterfaceType,configId));
        if(result.getCode()==1){
            return "redirect:"+result.getData();
//            .toString().replace(":8886",":8888").replace("//www.","//open.")
        }
        throw new IllegalArgumentException(result.getMsg());
    }
}

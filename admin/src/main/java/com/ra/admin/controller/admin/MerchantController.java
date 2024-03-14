package com.ra.admin.controller.admin;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.domain.Pager;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.*;
import com.ra.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/merchant")
public class MerchantController {
    @Autowired
    UserService userService;

    @Autowired
    MerchantInfoService merchantInfoService;

    @Autowired
    PayCodeService payCodeService;

    @Autowired
    private RateService rateService;

    @Autowired
    WalletService walletService;

    @Autowired
    BehalfBankCardGroupRelationService behalfBankCardGroupRelationService;

    @Autowired
    BehalfInfoService behalfInfoService;

    @Autowired
    BehalfBankCardGroupService behalfBankCardGroupService;
    /**
     * 商户列表
     * @param merchantQueryReq
     * @param model
     * @param pager
     * @return
     */
    @RequestMapping(value = "/findListMerchant", method = RequestMethod.GET)
    public String findListMerchant(@ModelAttribute MerchantQueryReq merchantQueryReq, Model model, @ModelAttribute Pager pager){

        Page<MerchantInfoVo> listByCondition = merchantInfoService.findListMerchantInfo(merchantQueryReq, pager);
        MerchantInfoTotalVo merchantTotal = merchantInfoService.findMerchantTotal(merchantQueryReq);
        model.addAttribute("listByCondition", listByCondition).addAttribute("merchantQueryReq", merchantQueryReq)
                .addAttribute("merchantTotal",merchantTotal).addAttribute("pager", pager);
        return "business/admin/merchant/list";

    }

    @GetMapping("/add")
    public String add(Model model){
        List<PayCode> allPayCode = payCodeService.getRepository().findAll();

        AtomicInteger index= new AtomicInteger(-1);
        List<PayCode2PayChannelBean> channelBeanList=allPayCode.stream().map(item->{
            PayCode2PayChannelBean bean=new PayCode2PayChannelBean();
            bean.setPayCodeId(item.getId());
            bean.setPayCodeName(item.getName());

            List<PayChannel> payChannelList = payCodeService.findPayChannelByPayCodeId(item.getId());

            List<RateVo> rateVoList=payChannelList.stream().map(channel->{
                RateVo vo=new RateVo();
                vo.setIndex(index.incrementAndGet());
                vo.setPayChannelId(channel.getId());
                vo.setPayChannelName(channel.getChannelName());
                vo.setProxyRate(channel.getRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                vo.setChannelRate(channel.getRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                vo.setChecked(false);
                vo.setWeight(100);
                return vo;
            }).collect(Collectors.toList());
            bean.setRateVoList(rateVoList);
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("bean",new AddMechantInfoVo());
        model.addAttribute("proxyList", userService.findAllProxyList());
        model.addAttribute("channelBeanList",channelBeanList);
        model.addAttribute("_method","POST");
        return "business/admin/merchant/form";
    }

    /**
     * 添加商户
     * @return
     */
    @PostMapping(value = "/add")
    public String addMerchant(Model model,@ModelAttribute AddMechantInfoVo addMechantInfoVo){
        try{
            Assert.notNull(addMechantInfoVo.getMerchantAccount(), "账号不能为空");
            Assert.notNull(addMechantInfoVo.getCompanyName(), "公司名称不能为空");
//        Assert.notNull(addMechantInfoVo.getProxyUserId(), "请选择通道代理");
            boolean i=userService.addMerchantUser(addMechantInfoVo, ApplyMerchantEnum.PASS);
            if(i){
                return "redirect:/private/merchant/findListMerchant";
            }
            throw new IllegalArgumentException("添加失败");
        }catch (Exception e){
            add(model);
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",addMechantInfoVo);
            model.addAttribute("_method","POST");
            return "business/admin/merchant/form";
        }
    }

    /**
     * 编辑
     * @param model
     * @param userId
     * @return
     */
    @GetMapping("/edit")
    public String edit(Model model,@RequestParam long userId){
        User user = userService.getRepository().findUserById(userId);
        Assert.notNull(user,"查询不到该商户");
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(userId);//查询商户信息
        Assert.notNull(user,"查询不到该商户信息");

        AddMechantInfoVo merchantInfoVo=new AddMechantInfoVo();
        merchantInfoVo.setCompanyName(merchantInfo.getCompanyName());
        merchantInfoVo.setMerchantAccount(user.getAccount());
        merchantInfoVo.setUserId(user.getId());
        merchantInfoVo.setProxyUserId(merchantInfo.getProxyUserId());

        List<Rate> rateList=rateService.getRepository().findByMerchantInfoId(merchantInfo.getId());

        AtomicInteger index= new AtomicInteger(-1);
        List<PayCode> allPayCode = payCodeService.getRepository().findAll();
        List<PayCode2PayChannelBean> channelBeanList=allPayCode.stream().map(item->{
            PayCode2PayChannelBean bean=new PayCode2PayChannelBean();
            bean.setPayCodeId(item.getId());
            bean.setPayCodeName(item.getName());

            List<PayChannel> payChannelList = payCodeService.findPayChannelByPayCodeId(item.getId());

            List<RateVo> rateVoList=payChannelList.stream().map(channel->{
                RateVo vo=new RateVo();
                vo.setIndex(index.incrementAndGet());
                vo.setPayChannelId(channel.getId());
                vo.setPayChannelName(channel.getChannelName());
                vo.setChannelRate(channel.getRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                vo.setProxyRate(channel.getRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                vo.setChecked(false);
                vo.setWeight(100);
                for (Rate rate : rateList) {
                    if(rate.getMerchantInfoId().longValue()==merchantInfo.getId().longValue()&&channel.getId().longValue()==rate.getPayChannelId().longValue()){
                        vo.setChecked(true);
                        vo.setMerchantRate(rate.getMerchantRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setProxyRate(rate.getProxyRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setMerchantInfoId(rate.getMerchantInfoId());
                        vo.setId(rate.getId());
                        vo.setWeight(rate.getWeight());
                    }
                }
                return vo;
            }).collect(Collectors.toList());
            bean.setRateVoList(rateVoList);
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("bean",merchantInfoVo);
        model.addAttribute("channelBeanList",channelBeanList);
        model.addAttribute("proxyList", userService.findAllProxyList());
        model.addAttribute("_method","PUT");
        return "business/admin/merchant/form";
    }

    @PutMapping("/edit")
    public String edit(Model model, @ModelAttribute AddMechantInfoVo addMechantInfoVo){
        try{
            Assert.notNull(addMechantInfoVo.getCompanyName(), "公司名称不能为空");
            boolean i=userService.editMerchantUser(addMechantInfoVo);
            if(i){
                return "redirect:/private/merchant/findListMerchant";
            }
            throw new IllegalArgumentException("修改失败");
        }catch (Exception e){
            edit(model,addMechantInfoVo.getUserId());
            model.addAttribute("message",e.getMessage());
            model.addAttribute("bean",addMechantInfoVo);
            model.addAttribute("_method","PUT");
            return "business/admin/merchant/form";
        }
    }

    /**
     * 禁用启用
     *
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        User user = userService.getRepository().findUserById(id);
        Assert.notNull(user,"查询不到该商户信息");

        if(user.getStatus()==0){
            user.setStatus(1);
        }else {
            user.setStatus(0);
        }
        userService.getRepository().save(user);
        return "redirect:/private/merchant/findListMerchant";
    }

    /**
     * 商户详情
     * @param userId
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "/findEditMerchant", method = RequestMethod.GET)
    public String findEditMerchant(@RequestParam("userId") Long userId, Model model, HttpServletRequest request){
        MerchantDetailVo merchantDetailVo=new MerchantDetailVo();
        /*查询用户信息*/
        User user = userService.getRepository().findUserById(userId);
        /*查询用户账户信息*/
        Wallet wallet=walletService.findWallet(userId);

        MerchantInfo merchantInfo=merchantInfoService.findByUserId(userId);

        User userProxy=userService.getRepository().findUserById(merchantInfo.getProxyUserId());
        merchantDetailVo.setMerchantUserId(userId);//商户ID
        merchantDetailVo.setMerchantAccount(user.getAccount());//商户账号
        merchantDetailVo.setCompanyName(merchantInfo.getCompanyName());//商户名称
        merchantDetailVo.setCreateTime(merchantInfo.getCreateTime());//注册时间
        merchantDetailVo.setAppId(merchantInfo.getAppId());//商户编号
        merchantDetailVo.setSecret(merchantInfo.getSecret());//商户秘钥
        merchantDetailVo.setStatus(user.getStatus());//商户状态

        if(userProxy!=null){
            merchantDetailVo.setProxyAccount(userProxy.getAccount());
        }else{
            merchantDetailVo.setProxyAccount("平台自招");
        }

        merchantDetailVo.setWalletMoney(wallet.getMoney());//账户总金额
        model.addAttribute("merchantDetailVo",merchantDetailVo).addAttribute("_method", "POST");
        return "business/admin/merchant/detail";
    }

    /**
     * 弹出修改金额
     * @param model
     * @return
     */
    @GetMapping("/updateMoney")
    public String updateMoney(Model model,@RequestParam("userId") Long userId){
        try{
            model.addAttribute("userId",userId);
            return "business/admin/merchant/updateMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchant/findListMerchant";
    }

    /**
     * 配置下发卡组
     * @param model
     * @param userId
     * @return
     */
    @GetMapping("/editBehalfCard")
    public String editBehalfCard(Model model,@RequestParam long userId){
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(userId);//查询商户信息
        Assert.notNull(merchantInfo,"查询不到该商户信息");

        List<BehalfBankCardGroupRelation>cardGroupRateList=behalfBankCardGroupRelationService.findByBehalfBankCardGroupRelationList(merchantInfo.getUserId());

        AtomicInteger index= new AtomicInteger(-1);
        List<BehalfInfo> allBehalfIno = behalfInfoService.findEnbleBehalfInfoList();
        List<BehalfBankCardGroupConfigBean> behalfCardGroupBeanList=allBehalfIno.stream().map(item->{
            BehalfBankCardGroupConfigBean bean =new BehalfBankCardGroupConfigBean();
            bean.setBehalfUserId(item.getUserId());
            bean.setBehalfName(item.getCompanyName());

            List<BehalfBankCardGroup>bankCardGroupList=behalfBankCardGroupService.findBankCardGroupList(item.getUserId(),true);

            List<CardGroupRateVo> rateVoList=bankCardGroupList.stream().map(cardGroup->{
                CardGroupRateVo cardGroupRateVo=new CardGroupRateVo();

                cardGroupRateVo.setIndex(index.incrementAndGet());
                cardGroupRateVo.setBankCardGroupId(cardGroup.getId());
                cardGroupRateVo.setCardGroupName(cardGroup.getCardGroupName());
                cardGroupRateVo.setBehalfRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                cardGroupRateVo.setBehalfFee(item.getBehalfFee());
                cardGroupRateVo.setChecked(false);
                cardGroupRateVo.setMerchantFee(item.getBehalfFee());
                cardGroupRateVo.setProxyFee(item.getBehalfFee());
                cardGroupRateVo.setMerchantRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                cardGroupRateVo.setProxyRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                for (BehalfBankCardGroupRelation rate : cardGroupRateList) {
                    if(rate.getMerchantUserId().longValue()==merchantInfo.getUserId().longValue()&&cardGroup.getId().longValue()==rate.getBankCardGroupId().longValue()){
                        cardGroupRateVo.setChecked(true);
                        cardGroupRateVo.setMerchantRate(rate.getMerchantRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        cardGroupRateVo.setProxyRate(rate.getProxyRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        cardGroupRateVo.setMerchantFee(rate.getMerchantFee());
                        cardGroupRateVo.setProxyFee(rate.getProxyFee());
                        cardGroupRateVo.setId(rate.getId());
                        cardGroupRateVo.setMerchantUserId(rate.getMerchantUserId());
                    }
                }
                return cardGroupRateVo;
            }).collect(Collectors.toList());
            bean.setCardGroupRateVoList(rateVoList);
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("userId",merchantInfo.getUserId());
        model.addAttribute("cardGroupBeanList",behalfCardGroupBeanList);
        model.addAttribute("_method","PUT");
        return "business/admin/merchant/cardConfigForm";
    }

    /**
     * 保存配置卡组
     * @param model
     * @param configMechantCardGroupVo
     * @return
     */
    @PutMapping("/editBehalfCard")
    public String editBehalfCard(Model model, @ModelAttribute ConfigMechantCardGroupVo configMechantCardGroupVo){
        try{
            boolean i=behalfBankCardGroupRelationService.editMerchantConfigCardGroup(configMechantCardGroupVo);
            if(i){
                return "redirect:/private/merchant/findListMerchant";
            }
            throw new IllegalArgumentException("配置失败");
        }catch (Exception e){
            editBehalfCard(model,configMechantCardGroupVo.getUserId());
            model.addAttribute("message",e.getMessage());
            model.addAttribute("_method","PUT");
            return "business/admin/merchant/cardConfigForm";
        }
    }
}

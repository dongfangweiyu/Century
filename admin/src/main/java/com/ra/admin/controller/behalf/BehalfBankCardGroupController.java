package com.ra.admin.controller.behalf;

import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.entity.business.BankList;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.entity.business.BehalfBankCardGroup;
import com.ra.dao.entity.security.User;
import com.ra.service.business.BankListService;
import com.ra.service.business.BehalfBankCardGroupService;
import com.ra.service.business.BehalfBankCardService;
import com.ra.service.business.ConfigPayInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 卡组管理
 */
@Controller
@RequestMapping("/private/bankCardGroup")
public class BehalfBankCardGroupController {

    @Autowired
    private BehalfBankCardGroupService behalfBankCardGroupService;
    @Autowired
    private BehalfBankCardService behalfBankCardService;
    @Autowired
    private BankListService bankListService;

    /**
     * 卡组列表
     * @param model
     * @return
     */
    @GetMapping("/list")
    public String list(Model model){
       List<BehalfBankCardGroup>list=behalfBankCardGroupService.findBankCardGroupList(TokenUtil.getLoginId(),false);
        model.addAttribute("list",list);
        return "business/behalf/bankCardGroup/list";
    }

    /**
     *  卡组 禁用启用
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    public String enable(@RequestParam("id") long id) {
        BehalfBankCardGroup behalfBankCardGroup=behalfBankCardGroupService.getRepository().findById(id);
        Assert.notNull(behalfBankCardGroup,"卡组不存在");

        if(behalfBankCardGroup.isEnable()){
            behalfBankCardGroup.setEnable(false);
        }else {
            behalfBankCardGroup.setEnable(true);
        }
        behalfBankCardGroupService.getRepository().save(behalfBankCardGroup);
        return "redirect:/private/bankCardGroup/list";
    }

    /**
     * 添加卡组
     * @param model
     * @return
     */
    @GetMapping("/addCardGroup")
    public String addCardGroup(Model model){
        model.addAttribute("behalfUserId",TokenUtil.getLoginId());
        model.addAttribute("_method","POST");
        return "business/behalf/bankCardGroup/cardGroupform";
    }

    /**
     * 添加卡组
     * @param behalfBankCardGroup
     * @return
     */
    @PostMapping("/addCardGroup")
    @ResponseBody
    public ApiResult addCardGroup(@ModelAttribute BehalfBankCardGroup behalfBankCardGroup){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(behalfBankCardGroup.getCardGroupName(),"卡组名称不能为空");

            behalfBankCardGroup.setEnable(true);
            behalfBankCardGroupService.getRepository().save(behalfBankCardGroup);
            return result.ok("添加成功");
        }catch (Exception e){
            System.out.println(e.getMessage());
            return result.fail(e.getMessage());
        }
    }

    /**
     *  所属卡组的代付银行卡列表
     * @param model
     * @return
     */
    @GetMapping("/configCard")
    public String config(Model model,@RequestParam("id") long id){
        List<BehalfBankCard> behalfBankCards =behalfBankCardService .findListBehalfBankCard(id);

        model.addAttribute("list",behalfBankCards);
        model.addAttribute("cardGroupId",id);
        model.addAttribute("_method","POST");
        return "business/behalf/bankCardGroup/configCard";
    }

    /**
     * 添加代付银行卡
     * @param model
     * @param id 卡组ID
     * @return
     */
    @GetMapping("/addBankCard")
    public String add(Model model,@RequestParam("id") long id){
        BehalfBankCard behalfBankCard=new BehalfBankCard();
        behalfBankCard.setBehalfGroupId(id);
        behalfBankCard.setBehalfUserId(TokenUtil.getLoginId());
        List<BankList> allBank = bankListService.getRepository().findAll();
        model.addAttribute("bean",behalfBankCard);
        model.addAttribute("allBank",allBank);
        model.addAttribute("_method","POST");
        return "business/behalf/bankCardGroup/bankCardForm";
    }

    /**
     * 添加代付银行卡
     * @param behalfBankCard
     * @return
     */
    @PostMapping("/addBankCard")
    @ResponseBody
    public ApiResult add(@ModelAttribute BehalfBankCard behalfBankCard){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(behalfBankCard.getBankName(),"银行名称不能为空");
            Assert.notNull(behalfBankCard.getRealName(),"持卡人姓名不能为空");
            Assert.hasText(behalfBankCard.getBankNo(),"卡号不能为空");
            BehalfBankCard  newBehalfBankCard=new BehalfBankCard();
            newBehalfBankCard.setBehalfGroupId(behalfBankCard.getBehalfGroupId());
            newBehalfBankCard.setBehalfUserId(behalfBankCard.getBehalfUserId());
            newBehalfBankCard.setBalance(BigDecimal.ZERO);//默认为0
            newBehalfBankCard.setComeIn(false);
            newBehalfBankCard.setComeOut(true);
            newBehalfBankCard.setBankNo(behalfBankCard.getBankNo());
            newBehalfBankCard.setBankName(behalfBankCard.getBankName());
            newBehalfBankCard.setRealName(behalfBankCard.getRealName());
            newBehalfBankCard.setBankBranch(behalfBankCard.getBankBranch());
            newBehalfBankCard.setRemark(behalfBankCard.getRemark());
            behalfBankCardService.getRepository().save(newBehalfBankCard);
            return result.ok("添加成功");
        }catch (Exception e){
            System.out.println(e.getMessage());
            return result.fail(e.getMessage());
        }
    }

    /**
     * 编辑代付银行卡
     * @param model
     * @param id
     * @return
     */
    @GetMapping("/edit")
    public String edit(Model model,@RequestParam(required = true) long id){
        BehalfBankCard behalfBankCard = behalfBankCardService.getRepository().findById(id);
        List<BankList> allBank = bankListService.getRepository().findAll();
        model.addAttribute("allBank",allBank);
        model.addAttribute("bean",behalfBankCard);
        model.addAttribute("_method","POST");
        return "business/behalf/bankCardGroup/bankCardForm";
    }

    /**
     * 编辑代付银行卡
     * @param behalfBankCard
     * @return
     */
    @PostMapping("/edit")
    @ResponseBody
    public ApiResult edit(@ModelAttribute BehalfBankCard behalfBankCard){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(behalfBankCard.getBankName(),"银行名称不能为空");
            Assert.notNull(behalfBankCard.getRealName(),"持卡人姓名不能为空");
            Assert.hasText(behalfBankCard.getBankNo(),"卡号不能为空");

            behalfBankCardService.getRepository().save(behalfBankCard);
            return result.ok("修改成功");
        }catch (Exception e){
            System.out.println(e.getMessage());
            return result.fail(e.getMessage());
        }
    }

    /**
     * 弹出修改卡金额
     * @param model
     * @return
     */
    @GetMapping("/modifyBankCardMoney")
    public String updateMoney(Model model,@RequestParam("id") Long id){
        BehalfBankCard behalfBankCard=behalfBankCardService.getRepository().findById((long)id);
        try{

            Assert.notNull(behalfBankCard,"银行卡不存在");
            model.addAttribute("id",id);
            return "business/behalf/bankCardGroup/updateMoney";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/bankCardGroup/configCard?id="+behalfBankCard.getBehalfGroupId();
    }

    /**
     * 修改金额
     * @param id 银行卡id
     * @param bankCardMoney 修改卡余额
     * @return
     */
    @PostMapping("/modifyBankCardMoney")
    @ResponseBody
    public ApiResult modifyWalletMoney(@RequestParam("id") Long id,
                                       @RequestParam("bankCardMoney") BigDecimal bankCardMoney,
                                       @RequestParam(value = "remark",required = false,defaultValue = "")String remark){
        ApiResult result=new ApiResult();
        try{
            Assert.notNull(id, "银行卡ID不能为空");
            Assert.notNull(bankCardMoney, "修改的金额不能为空");
            Assert.hasText(remark,"备注不能为空");

            User loginInfo = TokenUtil.getLoginInfo();
            BehalfWalletLogEnum behalfWalletLogEnum = BehalfWalletLogEnum.BEHALF;
            behalfWalletLogEnum.setDescription(remark+"（管理员："+loginInfo.getAccount()+"修改金额变动）");
            int i = behalfBankCardService.addMoney(id, bankCardMoney,behalfWalletLogEnum);
            if(i>0){
                return result.ok();
            }
            return result.fail("修改失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }

    @RequestMapping(value = "/removeBankCard", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult removeBankCard(@RequestParam("id") Long id) {
        ApiResult apiResult = new ApiResult();
        try {
            behalfBankCardService.getRepository().deleteById(id);
            apiResult.ok();
        } catch (Exception e) {
            apiResult.fail("删除失败");
        }
        return apiResult;
    }

    /**
     * 禁用启用
     * 是否打进
     * @return
     */
    @RequestMapping(value = "/enableComeIn", method = RequestMethod.GET)
    public String enableComeIn(@RequestParam("id") long id) {
        BehalfBankCard behalfBankCard=behalfBankCardService.getRepository().findById((long)id);
        Assert.notNull(behalfBankCard,"银行卡不存在");

        if(behalfBankCard.isComeIn()){
            behalfBankCard.setComeIn(false);
        }else {
            behalfBankCard.setComeIn(true);
        }
        behalfBankCardService.getRepository().save(behalfBankCard);
        return "redirect:/private/bankCardGroup/configCard?id="+behalfBankCard.getBehalfGroupId();
    }

    /**
     * 禁用启用
     *是否打出
     * @return
     */
    @RequestMapping(value = "/enableComeOut", method = RequestMethod.GET)
    public String enableComeOut(@RequestParam("id") long id) {
        BehalfBankCard behalfBankCard=behalfBankCardService.getRepository().findById((long)id);
        Assert.notNull(behalfBankCard,"银行卡不存在");

        if(behalfBankCard.isComeOut()){
            behalfBankCard.setComeOut(false);
        }else {
            behalfBankCard.setComeOut(true);
        }
        behalfBankCardService.getRepository().save(behalfBankCard);
        return "redirect:/private/bankCardGroup/configCard?id="+behalfBankCard.getBehalfGroupId();
    }

}

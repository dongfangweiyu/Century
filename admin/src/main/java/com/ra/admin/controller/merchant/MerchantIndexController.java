package com.ra.admin.controller.merchant;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.admin.utils.TokenUtil;
import com.ra.common.base.ApiResult;
import com.ra.common.component.GoogleAuthenticator;
import com.ra.common.utils.*;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.params.BehalfOrderCreateBankCardParams;
import com.ra.service.bean.params.OrderCreateParams;
import com.ra.service.bean.resp.*;
import com.ra.service.business.*;
import com.ra.service.component.UploadComponent;
import com.ra.service.security.UserService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/private/merchantIndex")
public class MerchantIndexController {
    @Value("${api.host}")
    private String apiHost;
    @Autowired
    UserService userService;
    @Autowired
    WalletService walletService;
    @Autowired
    MerchantInfoService merchantInfoService;

    @Autowired
    RateService rateService;
    @Autowired
    PayCodeService payCodeService;

    @Autowired
    BankCardService bankCardService;
    @Autowired
    WithdrawOrderService withdrawOrderService;
    @Autowired
    BehalfBankCardGroupRelationService behalfBankCardGroupRelationService;

    @Autowired
    BehalfInfoService behalfInfoService;

    @Autowired
    BehalfBankCardGroupService behalfBankCardGroupService;
    @Autowired
    BankListService bankListService;
    @Autowired
    PayOrderService payOrderService;

    /**
     * 商户首页
     * @param model
     * @return
     */
    @RequestMapping(value = "/findMerchantInfoIndex", method = RequestMethod.GET)
    public String findMerchantInfoIndex(Model model){
        long merchantUserId=TokenUtil.getLoginId();
        Wallet wallet=walletService.findWallet(merchantUserId);
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(merchantUserId);
        model.addAttribute("merchantInfo", merchantInfo).addAttribute("wallet", wallet);
        return "business/merchant/index/list";

    }

    /**
     * 弹出提现信息
     * @param model
     * @return
     */
    @GetMapping("/withdraw")
    public String withdraw(Model model){
        try{
            long merchantUserId=TokenUtil.getLoginId();
            User userById = userService.getRepository().findUserById(merchantUserId);
            List<BankCard>listBankCard=bankCardService.findBankCardList(merchantUserId);
            Wallet wallet=walletService.findWallet(merchantUserId);//余额
            model.addAttribute("wallet",wallet);
            model.addAttribute("listBankCard",listBankCard);
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("withdrawRate", ConfigFactory.getBigDecimal(ConfigEnum.WITHDRAW_RATE));
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("openPayPassword", !StringUtils.isEmpty(userById.getPay_password()));
            return "business/merchant/withdrawOrder/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }
    /**
     * 提现
     * @param orderMoney 提现金额
     * @param bankCardId 收款信息
     * @param fastGoogleAuthenticator 是否开启谷歌验证
     * @return
     */
    @RequestMapping(value = "/withdraw", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult withdraw(@RequestParam("orderMoney") Integer orderMoney,
                              @RequestParam("bankCardId")long bankCardId,
                              @RequestParam(required = false)Long fastGoogleAuthenticator,@RequestParam(value = "fastPayPassword",required=false)String fastPayPassword){
        ApiResult result=new ApiResult();
        try {
            Assert.notNull(orderMoney, "提现金额不能为空");
            Assert.notNull(bankCardId, "提现方式不能为空");
            if(orderMoney<100){
                throw new IllegalArgumentException("提现金额必须满足"+100+"元以上，谢谢！");
            }
            User userById =userService.getRepository().findUserById(TokenUtil.getLoginId());
            //如果开启了二次验证
            if(!StringUtils.isEmpty(userById.getGoogleAuthenticator())){
                Assert.notNull(fastGoogleAuthenticator,"谷歌验证随机码不能为空");
                long t = System.currentTimeMillis();
                GoogleAuthenticator ga = new GoogleAuthenticator();
                ga.setWindowSize(5);
                boolean r = ga.check_code(userById.getGoogleAuthenticator(), fastGoogleAuthenticator, t);
                if(!r){
                    throw new IllegalArgumentException("二次验证未通过");
                }
            }
            //如果开启了支付密码
            if(!StringUtils.isEmpty(userById.getPay_password())){
                Assert.notNull(fastPayPassword, "支付密码不能为空");
                String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
                if (!userById.getPay_password().equals(md5)) {
                    throw new IllegalArgumentException("支付密码输入错误!");
                }
            }

           BigDecimal withdrawRate=ConfigFactory.getBigDecimal(ConfigEnum.WITHDRAW_RATE);//提现手续费

            Wallet walletBean=walletService.findWallet(userById.getId());
            if(BigDecimal.valueOf(orderMoney).compareTo(walletBean.getMoney().subtract(withdrawRate))==1){
                throw new IllegalArgumentException("提现金额最大为："+walletBean.getMoney().subtract(withdrawRate).toPlainString()+"元");
            }
            WithdrawlOrder withdrawlOrder=new WithdrawlOrder();
            withdrawlOrder.setMerchantUserId(userById.getId());
            withdrawlOrder.setOrderNo(withdrawlOrder.genaralOrderNo("TX"));
            withdrawlOrder.setAmount(BigDecimal.valueOf(orderMoney));
            withdrawlOrder.setStatus(OrderStatusEnum.PROCESS);
            BankCard bankCard =bankCardService.getRepository().findById(bankCardId);
           /* String[] data = selectList.split(",");//提现方式
            for (String a : data) {
                long id = Long.parseLong(a);
                bankCard = bankCardService.getRepository().findById(id);
                if(bankCard==null){
                    return result.fail("银行卡添加异常,请刷新页面");
                }
            }*/
            String payJson = JSON.toJSONString(bankCard);
            withdrawlOrder.setBankCardInfoJson(payJson);
            boolean i = withdrawOrderService.addWithdrawal(withdrawlOrder);
            if (i) {
                return result.ok();
            }
            return result.fail("提现失败");
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }

    }

    /**
     * 商户查看自己的代收费率
     */
    @GetMapping("/findMerchentRate")
    public String findMerchentRate(Model model,@RequestParam long userId){
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(userId);//查询商户信息
        Assert.notNull(merchantInfo,"查询不到该商户信息");

        List<Rate> rateList=rateService.getRepository().findByMerchantInfoId(merchantInfo.getId());

        List<PayCode> allPayCode = payCodeService.getRepository().findAll();
        List<PayCode2PayChannelBean> channelBeanList=allPayCode.stream().map(item->{
            PayCode2PayChannelBean bean=new PayCode2PayChannelBean();
            bean.setPayCodeId(item.getId());
            bean.setPayCodeName(item.getName());

            List<PayChannel> payChannelList = payCodeService.findPayChannelByPayCodeId(item.getId());

            List<RateVo> rateVoList=payChannelList.stream().map(channel->{
                RateVo vo=new RateVo();
                vo.setPayChannelId(channel.getId());
                vo.setPayChannelName(channel.getChannelName());
                vo.setChecked(false);
                for (Rate rate : rateList) {
                    if(rate.getMerchantInfoId().longValue()==merchantInfo.getId().longValue()&&channel.getId().longValue()==rate.getPayChannelId().longValue()){
                        vo.setChecked(true);
                        vo.setMerchantRate(rate.getMerchantRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setProxyRate(rate.getProxyRate().multiply(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                        vo.setMerchantInfoId(rate.getMerchantInfoId());
                        vo.setId(rate.getId());
                    }
                }
                return vo;
            }).collect(Collectors.toList());
            bean.setRateVoList(rateVoList);
            return bean;
        }).collect(Collectors.toList());

        model.addAttribute("channelBeanList",channelBeanList);
        model.addAttribute("_method","PUT");
        return "business/merchant/index/form";
    }

    /**
     * 商户查看自己的代付费率
     */
    @GetMapping("/findMerchentBehalfRate")
    public String findMerchentBehalfRate(Model model,@RequestParam long userId){
        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(userId);//查询商户信息
        Assert.notNull(merchantInfo,"查询不到该商户信息");

        List<BehalfBankCardGroupRelation>cardGroupRateList=behalfBankCardGroupRelationService.findByBehalfBankCardGroupRelationList(merchantInfo.getUserId());

        AtomicInteger index= new AtomicInteger(-1);
        List<BehalfInfo> allBehalfIno = behalfInfoService.findEnbleBehalfInfoList();
        List<BehalfBankCardGroupConfigBean> behalfCardGroupBeanList=allBehalfIno.stream().map(item->{
            BehalfBankCardGroupConfigBean bean =new BehalfBankCardGroupConfigBean();
            bean.setBehalfUserId(item.getUserId());
            bean.setBehalfName(item.getCompanyName());

            List<BehalfBankCardGroup>bankCardGroupList=behalfBankCardGroupService.findMerchantBankCardGroupList(item.getUserId(),merchantInfo.getUserId());

            List<CardGroupRateVo> rateVoList=bankCardGroupList.stream().map(cardGroup->{
                CardGroupRateVo cardGroupRateVo=new CardGroupRateVo();

//                cardGroupRateVo.setIndex(index.incrementAndGet());
//                cardGroupRateVo.setBankCardGroupId(cardGroup.getId());
//                cardGroupRateVo.setCardGroupName(cardGroup.getCardGroupName());
//                cardGroupRateVo.setBehalfRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
//                cardGroupRateVo.setBehalfFee(item.getBehalfFee());
//                cardGroupRateVo.setChecked(false);
//                cardGroupRateVo.setMerchantFee(item.getBehalfFee());
//                cardGroupRateVo.setProxyFee(item.getBehalfFee());
//                cardGroupRateVo.setMerchantRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
//                cardGroupRateVo.setProxyRate(item.getBehalfRate().multiply(BigDecimal.valueOf(100)).setScale(4,BigDecimal.ROUND_UP));
                for (BehalfBankCardGroupRelation rate : cardGroupRateList) {
                    if(rate.getMerchantUserId().longValue()==merchantInfo.getUserId().longValue()&&cardGroup.getId().longValue()==rate.getBankCardGroupId().longValue()){
                        cardGroupRateVo.setIndex(index.incrementAndGet());
                        cardGroupRateVo.setBankCardGroupId(cardGroup.getId());
                        cardGroupRateVo.setCardGroupName(cardGroup.getCardGroupName());
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

        model.addAttribute("cardGroupBeanList",behalfCardGroupBeanList);
        model.addAttribute("_method","PUT");
        return "business/merchant/index/behalfForm";
    }

    /**
     * 弹出手动充值信息
     * @param model
     * @return
     */
    @GetMapping("/manualPay")
    public String manualPay(Model model){
        try{
            return "business/merchant/manualPay/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }
    /**
     * 点击手动充值跳转充值页面
     * @return
     */
    @RequestMapping(value = "/manualPay", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult manualPay(HttpServletRequest request, @ModelAttribute OrderCreateParams params){
        ApiResult apiResult=new ApiResult();
        Assert.notNull(params.getAmount(),"充值金额不能为空");

        String url =apiHost+"/pay/create";

        Long timestamp=System.currentTimeMillis();

        MerchantInfo merchantInfo = merchantInfoService.getRepository().findByUserId(TokenUtil.getLoginId());
        Assert.notNull(merchantInfo,"商户信息不存在，无法手动充值");

        params.setAppId(merchantInfo.getAppId());
        params.setAttach("");
        params.setNonceStr(NumberUtil.generateCharacterString(18).toUpperCase());
        params.setOrderDesc("手动充值");
        params.setNotifyUrl(apiHost+"/notify");
        params.setReturnUrl("");
        params.setPayCode("bankcard");
        params.setOutOrderNo(NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
        //params.setOutOrderNo("123456");
        params.setTimestamp(timestamp);
        params.setSignature(SignUtils.generateCreateSign(params.getOutOrderNo(),params.getAmount(),params.getPayCode(),params.getAttach()
                ,params.getAppId(), params.getTimestamp(),params.getNonceStr(), merchantInfo.getSecret()));

        String result = HttpUtil.doPOST(url, autoBeanToMap(params));
        if(!StringUtils.isEmpty(result)){
            JSONObject object=JSONObject.parseObject(result);
            if(object.getIntValue("code")==1){
               return apiResult.ok().inject(object.getString("data"));
            }
            return apiResult.fail(object.getString("msg"));
        }
        throw new IllegalArgumentException("请求通道服务器失败。");

    }

    /**
     * 弹出手动下代付订单
     * @param model
     * @return
     */
    @GetMapping("/manualBehalf")
    public String manualBehalf(Model model){
        try{
            long merchantUserId=TokenUtil.getLoginId();
            User userById = userService.getRepository().findUserById(merchantUserId);
            List<BankList>listCard= bankListService.getRepository().findAll();
            model.addAttribute("listCard",listCard);
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("openPayPassword", !StringUtils.isEmpty(userById.getPay_password()));
            return "business/merchant/manualBehalf/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }
    /**
     * 点击手动下发代付跳转下发页面
     * @return
     */
    @RequestMapping(value = "/manualBehalf", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult manualBehalf(HttpServletRequest request, @RequestParam BigDecimal amount,
                               @ModelAttribute BehalfOrderCreateBankCardParams bankCardParams,
                                  @RequestParam(required = false)Long fastGoogleAuthenticator,@RequestParam(value = "fastPayPassword",required=false)String fastPayPassword){
        ApiResult apiResult=new ApiResult();

        Assert.notNull(amount,"代付金额不能为空");

        User userById =userService.getRepository().findUserById(TokenUtil.getLoginId());
        //如果开启了二次验证
        if(!StringUtils.isEmpty(userById.getGoogleAuthenticator())){
            Assert.notNull(fastGoogleAuthenticator,"谷歌验证随机码不能为空");
            long t = System.currentTimeMillis();
            GoogleAuthenticator ga = new GoogleAuthenticator();
            ga.setWindowSize(5);
            boolean r = ga.check_code(userById.getGoogleAuthenticator(), fastGoogleAuthenticator, t);
            if(!r){
                throw new IllegalArgumentException("二次验证未通过");
            }
        }
        //如果开启了支付密码
        if(!StringUtils.isEmpty(userById.getPay_password())){
            Assert.notNull(fastPayPassword, "支付密码不能为空");
            String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
            if (!userById.getPay_password().equals(md5)) {
                throw new IllegalArgumentException("支付密码输入错误!");
            }
        }

        String url = apiHost+"/behalf/create";
        Long timestamp=System.currentTimeMillis();

        MerchantInfo merchantInfo = merchantInfoService.getRepository().findByUserId(userById.getId());
        Assert.notNull(merchantInfo,"商户信息不存在,无法手动发起代付。");

        Map<String,Object> params=new HashMap<>();
        params.put("appId",merchantInfo.getAppId());
        params.put("amount",amount);
        params.put("attach","");
        params.put("nonceStr",NumberUtil.generateCharacterString(18).toUpperCase());
        params.put("notifyUrl",apiHost+"/notify");
        params.put("outOrderNo",NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
        params.put("timestamp",timestamp);
        params.put("signature",SignUtils.generateCreateSign(params.get("outOrderNo").toString(),
                amount,null,params.get("attach").toString()
                ,params.get("appId").toString(), timestamp,params.get("nonceStr").toString(), merchantInfo.getSecret()));
        params.put("bankCard.bankName",bankCardParams.getBankName());
        params.put("bankCard.bankNo",bankCardParams.getBankNo());
        params.put("bankCard.realName",bankCardParams.getRealName());
        params.put("bankCard.bankBranch",bankCardParams.getBankBranch());

        String result = HttpUtil.doPOST(url, params);
        if(!StringUtils.isEmpty(result)){
            JSONObject object=JSONObject.parseObject(result);
            if(object.getIntValue("code")==1){
                return apiResult.ok(object.getString("msg"));
            }
            return apiResult.fail(object.getString("msg"));
        }
        throw new IllegalArgumentException("请求通道服务器失败。");

    }

    /**
     * 弹出手动批量下代付订单
     * @param model
     * @return
     */
    @GetMapping("/batchBehalf")
    public String batchBehalf(Model model){
        try{
            List<BatchBehalfDataVo> allBatchBehalfData = new ArrayList<>();
            BatchBehalfDataVo batchBehalfDataVo;
            for(int i=1;i<=10;i++){
                batchBehalfDataVo=new BatchBehalfDataVo();
                batchBehalfDataVo.setIndex(i);
                batchBehalfDataVo.setChecked(false);
                allBatchBehalfData.add(batchBehalfDataVo);
            }
            long merchantUserId=TokenUtil.getLoginId();
            User userById = userService.getRepository().findUserById(merchantUserId);
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("openPayPassword", !StringUtils.isEmpty(userById.getPay_password()));
            model.addAttribute("allBatchBehalfData", allBatchBehalfData);
            return "business/merchant/batchBehalf/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }

    /**
     * 点击手动批量下代付订单
     * @return
     */
    @RequestMapping(value = "/batchBehalf", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult batchBehalf(HttpServletRequest request, @ModelAttribute BatchBehalfOrderDataVo batchBehalfOrderDataVo,
                                  @RequestParam(required = false)Long fastGoogleAuthenticator,@RequestParam(value = "fastPayPassword",required=false)String fastPayPassword){
        ApiResult apiResult=new ApiResult();
        try {
            User userById = userService.getRepository().findUserById(TokenUtil.getLoginId());
            //如果开启了二次验证
            if (!StringUtils.isEmpty(userById.getGoogleAuthenticator())) {
                Assert.notNull(fastGoogleAuthenticator, "谷歌验证随机码不能为空");
                long t = System.currentTimeMillis();
                GoogleAuthenticator ga = new GoogleAuthenticator();
                ga.setWindowSize(5);
                boolean r = ga.check_code(userById.getGoogleAuthenticator(), fastGoogleAuthenticator, t);
                if (!r) {
                    throw new IllegalArgumentException("二次验证未通过");
                }
            }
            //如果开启了支付密码
            if (!StringUtils.isEmpty(userById.getPay_password())) {
                Assert.notNull(fastPayPassword, "支付密码不能为空");
                String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
                if (!userById.getPay_password().equals(md5)) {
                    throw new IllegalArgumentException("支付密码输入错误!");
                }
            }
            boolean i = merchantInfoService.batchBehalfOrder(batchBehalfOrderDataVo, userById.getId(),apiHost+"/notify");
            if (i) {
                return apiResult.ok();
            } else {
                return apiResult.fail("批量下单失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return apiResult.fail(e.getMessage());
        }
    }
    /**
     * 应用反射(其实工具类底层一样用的反射技术)
     * 手动写一个 Bean covert to Map
     */
    public static Map<String,Object> autoBeanToMap(Object params){
        Map<String,Object> keyValues=new HashMap<>();

        Method[] methods=params.getClass().getMethods();
        try {
            for(Method method: methods){

                String methodName=method.getName();
                //反射获取属性与属性值的方法很多，以下是其一；也可以直接获得属性，不过获取的时候需要用过设置属性私有可见
                if (methodName.contains("get")){
                    //invoke 执行get方法获取属性值
                    Object value=method.invoke(params);
                    //根据setXXXX 通过以下算法取得属性名称
                    String key=methodName.substring(methodName.indexOf("get")+3);
                    Object temp=key.substring(0,1).toString().toLowerCase();
                    key=key.substring(1);
                    //最终得到属性名称
                    key=temp+key;
                    keyValues.put(key,value);
                }
            }
        }catch (Exception e){
        }
        return keyValues;
    }
    /**
     * 绑定下单IP
     * @param model
     * @return
     */
    @GetMapping("/editBehalfIp")
    public String editBehalfIp(Model model){
        try{
            MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(TokenUtil.getLoginId());
            Assert.notNull(merchantInfo,"商户信息不存在，无法绑定");
            model.addAttribute("merchantInfo", merchantInfo);
            return "business/merchant/behalfIp/form";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }
    /**
     * 绑定下单IP
     * @return
     */
    @RequestMapping(value = "/editBehalfIp", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult editBehalfIp( @RequestParam String behalfIp){
        ApiResult apiResult=new ApiResult();

        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(TokenUtil.getLoginId());
        Assert.notNull(merchantInfo,"商户信息不存在，无法绑定");

        if(StringUtils.hasText(behalfIp)){
            if(IpUtil.isIp(behalfIp)){
                merchantInfo.setBehalfIp(behalfIp);
            }else{
                return apiResult.fail("IP格式不正确");
            }
        }else{
            merchantInfo.setBehalfIp(null);
        }
        merchantInfoService.getRepository().save(merchantInfo);
        return apiResult.ok();

    }

    /**
     * 上传批量文本文件
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult uploadFile(MultipartHttpServletRequest request) {
        ApiResult result = new ApiResult();
        try {
            MultipartFile file = request.getFile("file");
            String upload = UploadComponent.upload(file, UploadComponent.UploadTypeEnum.bulkText);
            Map<String, Object> params = new HashMap<>();
            params.put("path",  upload);
            return result.ok().inject(params);
        } catch (Exception e) {
            return result.fail("上传失败");
        }
    }

    /**
     * 批量文本导入成功后执行批量下单操作
     * @return
     */
    @PostMapping(value = "/bulkText")
    @ResponseBody
    public ApiResult bulkText(@RequestParam String textPath) {
        ApiResult result=new ApiResult();
        try{
            Assert.hasText(textPath,"未找到执行的批量文本");

            List<BatchBehalfDataVo> allBatchBehalfData=new ArrayList<>();
            BatchBehalfDataVo batchBehalfData;
            Workbook wb =null;
            Sheet sheet = null;
            Row row = null;
            List<Map<String,String>> list = null;
            String cellData = null;
            //String filePath = "D:\\test.xlsx";
            String columns[] = {"realName","bankName","bankNo","bankBranch","amount"};
            wb = readExcel(PropertyUtil.getValue("spring.servlet.multipart.location")+ File.separator+textPath);
            if(wb != null){
                //用来存放表中数据
                list = new ArrayList<Map<String,String>>();
                //获取第一个sheet
                sheet = wb.getSheetAt(0);
                //获取最大行数
                int rownum = sheet.getPhysicalNumberOfRows();
                //获取第一行
                row = sheet.getRow(0);
                //获取最大列数
                int colnum = row.getPhysicalNumberOfCells();
                for (int i = 1; i<rownum; i++) {
                    //Map<String,String> map = new LinkedHashMap<String,String>();
                    row = sheet.getRow(i);
                    if(row !=null){
                        batchBehalfData=new BatchBehalfDataVo();
                        for (int j=0;j<colnum;j++){
                            cellData = (String) getCellFormatValue(row.getCell(j));
                            if(j==0){//验证持卡人姓名是否有空的
                                Assert.hasText(cellData,"批量文本中有持卡人姓名为空,请补充");
                                batchBehalfData.setRealName(cellData);
                            }else if(j==1){//验证开户行是否有空值
                                Assert.hasText(cellData,"批量文本中有开户行信息为空,请补充");
                                batchBehalfData.setBankName(cellData);
                            } else if(j==2){//验证卡号是否有空值
                                Assert.hasText(cellData,"批量文本中有银行卡号为空,请补充");
                                batchBehalfData.setBankNo(cellData);
                            }else if(j==3){//支行信息
                                batchBehalfData.setBankBranch(cellData);
                            } else if(j==4){//验证下单金额是否有空值
                                Assert.hasText(cellData,"批量文本中有下单金额为空,请补充");
                                BigDecimal intAmount =BigDecimal.valueOf(Double.parseDouble(cellData)).setScale(2,BigDecimal.ROUND_DOWN);
                                if(intAmount.compareTo(BigDecimal.ZERO)<=0){
                                    throw new IllegalArgumentException("批量文本下单的金额必须大于0");
                                }
                                batchBehalfData.setAmount(intAmount);
                            }
                        }
                        allBatchBehalfData.add(batchBehalfData);
                    }else{
                        break;
                    }
                }
            }
            boolean i = merchantInfoService.bulkTextBehalfOrder(allBatchBehalfData, TokenUtil.getLoginId(),apiHost+"/notify");
            if (i) {
                return result.ok();
            } else {
                return result.fail("批量下单失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return result.fail(e.getMessage());
        }
    }


    //读取excel
    public static Workbook readExcel(String filePath){
        Workbook wb = null;
        if(filePath==null){
            return null;
        }
        String extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            if(".xls".equals(extString)){
                return wb = new HSSFWorkbook(is);
            }else if(".xlsx".equals(extString)){
                return wb = new XSSFWorkbook(is);
            }else{
                return wb = null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wb;
    }
    public static Object getCellFormatValue(Cell cell){
        Object cellValue = null;
        if(cell!=null){
            //判断cell类型
            switch(cell.getCellType()){
                case Cell.CELL_TYPE_NUMERIC:{
                    cellValue = String.valueOf(cell.getNumericCellValue());
                    break;
                }
                case Cell.CELL_TYPE_STRING:{
                    cellValue = cell.getRichStringCellValue().getString();
                    break;
                }
                default:
                    cellValue = "";
            }
        }else{
            cellValue = "";
        }
        return cellValue;
    }

    /**
     * 弹出导入文本支付密码与谷歌验证的校验
     * @param model
     * @return
     */
    @GetMapping("/bulkTextCheck")
    public String bulkTextCheck(Model model){
        try{
            long merchantUserId=TokenUtil.getLoginId();
            User userById = userService.getRepository().findUserById(merchantUserId);
            model.addAttribute("openGoogleAuthenticator",!StringUtils.isEmpty(userById.getGoogleAuthenticator()));
            model.addAttribute("openPayPassword", !StringUtils.isEmpty(userById.getPay_password()));
            return "business/merchant/index/bulkCheckForm";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }

    /**
     * 导入文本支付密码与谷歌验证的校验
     * @return
     */
    @RequestMapping(value = "/bulkTextCheck", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult bulkTextCheck(HttpServletRequest request, @RequestParam(required = false)Long fastGoogleAuthenticator,@RequestParam(value = "fastPayPassword")String fastPayPassword){
        ApiResult apiResult=new ApiResult();
        try {
            User userById = userService.getRepository().findUserById(TokenUtil.getLoginId());
            Assert.hasText(fastPayPassword,"请先绑定支付密码或谷歌验证");
            //如果开启了二次验证
            if (!StringUtils.isEmpty(userById.getGoogleAuthenticator())) {
                Assert.notNull(fastGoogleAuthenticator, "谷歌验证随机码不能为空");
                long t = System.currentTimeMillis();
                GoogleAuthenticator ga = new GoogleAuthenticator();
                ga.setWindowSize(5);
                boolean r = ga.check_code(userById.getGoogleAuthenticator(), fastGoogleAuthenticator, t);
                if (!r) {
                    throw new IllegalArgumentException("二次验证未通过");
                }
            }
            //如果开启了支付密码
            if (!StringUtils.isEmpty(userById.getPay_password())) {
                Assert.notNull(fastPayPassword, "支付密码不能为空");
                String md5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(fastPayPassword));
                if (!userById.getPay_password().equals(md5)) {
                    throw new IllegalArgumentException("支付密码输入错误!");
                }
            }
                return apiResult.ok();
        }catch (Exception e){
            e.printStackTrace();
            return apiResult.fail(e.getMessage());
        }
    }

    /**
     * 弹出导入文本支付密码与谷歌验证的校验
     * @param model
     * @return
     */
    @GetMapping("/bulkTextFile")
    public String bulkTextFile(Model model){
        try{
            return "business/merchant/index/bulkTextFile";
        }catch (Exception e){
            model.addAttribute("message",e.getMessage());
        }
        return "redirect:/private/merchantIndex/findMerchantInfoIndex";
    }

}

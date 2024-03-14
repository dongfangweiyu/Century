package com.ra.open.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ra.common.bean.ExtraData;
import com.ra.common.utils.HttpUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.NumberUtil;
import com.ra.common.utils.SignUtils;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.params.BehalfOrderCreateBankCardParams;
import com.ra.service.bean.params.OrderCreateParams;
import com.ra.service.business.BankListService;
import com.ra.service.business.BehalfInfoService;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.PayCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class TestController {

	@Autowired
	private PayCodeService payCodeService;
	@Autowired
	private BankListService bankListService;
	@Autowired
	private MerchantInfoService merchantInfoService;
	@Autowired
	private BehalfInfoService behalfInfoService;

	//定义一个全局的记录器，通过LoggerFactory获取
    private final static Logger logger = LoggerFactory.getLogger(TestController.class);

	@GetMapping(value = "/")
	public String demo(Model model){
		if(ConfigFactory.getBoolean(ConfigEnum.SYSTEM_DEMO)){
			List<PayCode> allPayCode = payCodeService.getRepository().findAll();
			List<BankList> allBank = bankListService.getRepository().findAll();
			model.addAttribute("allPayCode",allPayCode);
			model.addAttribute("allBank",allBank);
			return "demo";
		}
        model.addAttribute("message","演示DEMO已关闭");
		return "error";
	}

	/**
	 * 请求支付
	 * @return
	 */
	@PostMapping("/pay")
	public String pay(HttpServletRequest request,@ModelAttribute OrderCreateParams params) {
		if(!ConfigFactory.getBoolean(ConfigEnum.SYSTEM_DEMO)){
			throw new IllegalArgumentException("非法请求");
		}
		Assert.notNull(params.getAmount(),"测试金额不能为空");
		Assert.notNull(params.getPayCode(),"支付类型不能为空");

		String url = IpUtil.getHost(request)+"/pay/create";

		Long timestamp=System.currentTimeMillis();

		MerchantInfo merchantInfo = merchantInfoService.getRepository().findMerchantInfoById(1l);
		Assert.notNull(merchantInfo,"演示数据已被移除，无法演示");

		params.setAppId(merchantInfo.getAppId());
		params.setAttach("");
		params.setNonceStr(NumberUtil.generateCharacterString(18).toUpperCase());
		params.setOrderDesc("演示支付");
		params.setNotifyUrl(IpUtil.getHost(request)+"/notify");
		params.setReturnUrl(IpUtil.getHost(request)+"/");
		params.setOutOrderNo(NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
		//params.setOutOrderNo("123456");
		params.setTimestamp(timestamp);
		params.setSignature(SignUtils.generateCreateSign(params.getOutOrderNo(),params.getAmount(),params.getPayCode(),params.getAttach()
				,params.getAppId(), params.getTimestamp(),params.getNonceStr(), merchantInfo.getSecret()));

		String result = HttpUtil.doPOST(url, autoBeanToMap(params));
		if(!StringUtils.isEmpty(result)){
			logger.info("result="+result);
			JSONObject object=JSONObject.parseObject(result);
			if(object.getIntValue("code")==1){
				return "redirect:" + object.getString("data");
			}
			throw new IllegalArgumentException(object.getString("msg"));
		}
		throw new IllegalArgumentException("请求通道服务器失败。");
	}


	/**
	 * 请求代付
	 * @return
	 */
	@PostMapping("/behalf")
	public String behalf(HttpServletRequest request, @RequestParam BigDecimal amount,
						 @ModelAttribute BehalfOrderCreateBankCardParams bankCardParams) {
		if(!ConfigFactory.getBoolean(ConfigEnum.SYSTEM_DEMO)){
			throw new IllegalArgumentException("非法请求");
		}
		Assert.notNull(amount,"测试金额不能为空");

		String url = IpUtil.getHost(request)+"/behalf/create";

		Long timestamp=System.currentTimeMillis();

		MerchantInfo merchantInfo = merchantInfoService.getRepository().findMerchantInfoById(1l);
		Assert.notNull(merchantInfo,"演示数据已被移除，无法演示");

		Map<String,Object> params=new HashMap<>();
		params.put("appId",merchantInfo.getAppId());
		params.put("amount",amount);
		params.put("attach","");
		params.put("nonceStr",NumberUtil.generateCharacterString(18).toUpperCase());
		params.put("notifyUrl",IpUtil.getHost(request)+"/notify");
		params.put("outOrderNo",NumberUtil.generateTimeStrapFormat()+NumberUtil.generateDigitalString(4));
		params.put("timestamp",timestamp);
		params.put("signature",SignUtils.generateCreateSign(params.get("outOrderNo").toString(),
				amount,null,params.get("attach").toString()
				,params.get("appId").toString(), timestamp,params.get("nonceStr").toString(), merchantInfo.getSecret()));
		params.put("bankCard.bankName",bankCardParams.getBankName());
		params.put("bankCard.bankNo",bankCardParams.getBankNo());
		params.put("bankCard.realName",bankCardParams.getRealName());
		params.put("bankCard.bankBranch","");

		String result = HttpUtil.doPOST(url, params);
		if(!StringUtils.isEmpty(result)){
			logger.info("result="+result);
			JSONObject object=JSONObject.parseObject(result);
			if(object.getIntValue("code")==1){
				return "redirect:" + object.getString("data");
			}
			throw new IllegalArgumentException(object.getString("msg"));
		}
		throw new IllegalArgumentException("请求通道服务器失败。");
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
	 * 异步通知
	 */
	@PostMapping("/notify")
	@ResponseBody
	public Object notify(HttpServletRequest request) {
		logger.info("收到支付结果的异步通知...");
		logger.info("OrderNo:" + request.getParameter("orderNo"));
		logger.info("outOrderNo:"+request.getParameter("outOrderNo"));
		logger.info("Signature:" + request.getParameter("signature"));
		logger.info("payCode:"+request.getParameter("payCode"));
		logger.info("amount:"+request.getParameter("amount"));
		logger.info("status:"+request.getParameter("status"));
		logger.info("nonceStr:" + request.getParameter("nonceStr"));
		logger.info("Timestamp:" + request.getParameter("timestamp"));
		logger.info("remark:"+request.getParameter("remark"));

		return "SUCCESS";
//		String outOrderNo=request.getParameter("outOrderNo");
//		BigDecimal amount=new BigDecimal(request.getParameter("amount"));
//		String payCode=request.getParameter("payCode");
//		String attach=request.getParameter("attach");
//		Long timestamp=Long.parseLong(request.getParameter("timestamp"));
//		String nonceStr=request.getParameter("nonceStr");
//		String signature=request.getParameter("signature");
//
//		MerchantInfo merchantInfo = merchantInfoService.getRepository().findMerchantInfoById(1l);
//		Assert.notNull(merchantInfo,"演示数据已被移除，无法演示");
//		try{
//			// 延签
//			// 验证是不是支付网关发来的异步通知
//			String sign=SignUtils.generateCreateSign(outOrderNo,amount,payCode,attach,merchantInfo.getAppId(), timestamp,nonceStr,merchantInfo.getSecret() );
//			if (signature.equals(sign)) {
//				logger.info("验签通过...");
//				// 这里商户写自己的业务逻辑
//				//
//				//
//				//
//				//
//
//				return "SUCCESS";
//			}
//			logger.info("验签通过...");
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//		return "FAIL";
	}


	/**
	 * 查询订单
	 * @return
	 */
	@RequestMapping("/query/{orderNo}")
	@ResponseBody
	public String query(HttpServletRequest request,@PathVariable("orderNo") String orderNo) {
		if(!ConfigFactory.getBoolean(ConfigEnum.SYSTEM_DEMO)){
			throw new IllegalArgumentException("非法请求");
		}

		String url = IpUtil.getHost(request)+"/pay/query";
		if(orderNo.contains("DF")){
			url= IpUtil.getHost(request)+"/behalf/query";
		}

		MerchantInfo merchantInfo = merchantInfoService.getRepository().findMerchantInfoById(1l);
		Assert.notNull(merchantInfo,"演示数据已被移除，无法演示");

		Long timestamp=System.currentTimeMillis();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("orderNo", orderNo);// 金额
//		params.put("outOrderNo", orderNo);
		params.put("timestamp", timestamp);// 时间戳
		params.put("appId", merchantInfo.getAppId());// 商户ID
		String nonceStr= NumberUtil.generateCharacterString(18).toUpperCase();
		params.put("nonceStr",nonceStr);//业务流水号，18位长度以上的随机字符串
		params.put("signature", SignUtils.generateQuerySign(merchantInfo.getAppId(), timestamp,nonceStr, merchantInfo.getSecret()));// 签名

		String result = HttpUtil.doPOST(url, params);
		return result;
	}

	/**
	 * 卡商查询未处理的代付订单
	 * @return
	 */
	@RequestMapping("/queryBehalfOrderList")
	@ResponseBody
	public String queryBehalfOrderList(HttpServletRequest request) {
		if(!ConfigFactory.getBoolean(ConfigEnum.SYSTEM_DEMO)){
			throw new IllegalArgumentException("非法请求");
		}
		String url = IpUtil.getHost(request)+"/behalf/behalfQueryList";

		BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId((long)78);
		Assert.notNull(behalfInfo,"演示数据已被移除，无法演示");

		Long timestamp=System.currentTimeMillis();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("timestamp", timestamp);// 时间戳
		params.put("appId", behalfInfo.getAppId());// 卡商ID
		String nonceStr= NumberUtil.generateCharacterString(18).toUpperCase();
		params.put("nonceStr",nonceStr);//业务流水号，18位长度以上的随机字符串
		params.put("signature", SignUtils.generateQuerySign(behalfInfo.getAppId(), timestamp,nonceStr, behalfInfo.getSecret()));// 签名

		String result = HttpUtil.doPOST(url, params);
		return result;
	}

	/**
	 * 处理订单
	 * @return
	 */
	@RequestMapping("/automaticConfirm/{orderNo}&{auditRequest}")
	@ResponseBody
	public String automaticConfirm(HttpServletRequest request,@PathVariable("orderNo") String orderNo,@PathVariable("auditRequest") String auditRequest) {
		String url = IpUtil.getHost(request)+"/behalf/automaticConfirmOrder";


		BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId((long)78);
		Assert.notNull(behalfInfo,"演示数据已被移除，无法演示");

		Long timestamp=System.currentTimeMillis();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("orderNo", orderNo);// 金额
		params.put("amount", BigDecimal.valueOf(100));
		params.put("timestamp", timestamp);// 时间戳
		params.put("appId", behalfInfo.getAppId());// 商户ID
		String nonceStr= NumberUtil.generateCharacterString(18).toUpperCase();
		params.put("nonceStr",nonceStr);//业务流水号，18位长度以上的随机字符串
		params.put("auditRequest",auditRequest);//确认传confirm 驳回传reject
		params.put("signature", SignUtils.generateAutomaticConfirmSign(orderNo,BigDecimal.valueOf(100),behalfInfo.getAppId(), timestamp,nonceStr, behalfInfo.getSecret()));// 签名

		String result = HttpUtil.doPOST(url, params);
		return result;
	}

	public static void main(String[] args) {

//		OrderCreateParams params=new OrderCreateParams();
//		params.setAppId("20191113154437106344");
//		params.setAttach("192zxxdfqqq1232xeq56");
//		params.setNonceStr("192zxxdfqqq1232xeq56");
//		params.setOrderDesc("美的洗衣机");
//		params.setAmount(BigDecimal.valueOf(333));
//		params.setNotifyUrl("http://pjnotify.yqf178.com/notify");
//		params.setReturnUrl("http://pjnotify.yqf178.com/notify");
//		params.setPayCode("ALIPAY_SCA");
//		params.setOutOrderNo("701610157");
//		params.setTimestamp(1573717836565l);
//		params.setSignature("883E151682724D856001142AD3082606");
//
//		String s1 = SignUtils.generateCreateSign("701610157",BigDecimal.valueOf(333),"ALIPAY_SCA","192zxxdfqqq1232xeq56",
//				"20191113154437106344",1573717836565l,"192zxxdfqqq1232xeq56","");
//		System.out.println("签名："+s1);


		String json="{\"extraData\":\"{\\\"convertClass\\\":\\\"com.ra.service.channel.BehalfInterface\\\",\\\"data\\\":{\\\"balance\\\":18316.00,\\\"bankBranch\\\":\\\"\\\",\\\"bankName\\\":\\\"中国工商银行\\\",\\\"bankNo\\\":\\\"6222021911005603292\\\",\\\"behalfGroupId\\\":17,\\\"behalfUserId\\\":145,\\\"comeIn\\\":true,\\\"comeOut\\\":true,\\\"createTime\\\":1603714225000,\\\"deleted\\\":false,\\\"id\\\":23,\\\"merchantFee\\\":1.00,\\\"merchantRate\\\":0.0070,\\\"proxyRate\\\":0.0030,\\\"realName\\\":\\\"肖都\\\",\\\"remark\\\":\\\"\\\"}}\",\"rateInfoJson\":\"{\\\"createTime\\\":1603522842000,\\\"deleted\\\":false,\\\"id\\\":5650,\\\"merchantInfoId\\\":104,\\\"merchantRate\\\":0.0000,\\\"payChannelId\\\":163,\\\"proxyRate\\\":0.0000,\\\"weight\\\":100}\",\"notifyCount\":1,\"successTime\":1603724554000,\"id\":699418,\"attach\":\"\",\"returnUrl\":\"\",\"orderDesc\":\"手动充值\",\"payChannelId\":163,\"payChannelInfoJson\":\"{\\\"channelHost\\\":\\\"1\\\",\\\"channelName\\\":\\\"银行卡代付充值\\\",\\\"configPayInterfaceId\\\":91,\\\"createTime\\\":1603087241000,\\\"creditScore\\\":0.00,\\\"deleted\\\":false,\\\"enable\\\":true,\\\"id\\\":163,\\\"payInterface\\\":\\\"BEHALF\\\",\\\"payInterfaceType\\\":\\\"BANKCARD\\\",\\\"rate\\\":0.000,\\\"risk\\\":false,\\\"weight\\\":100}\",\"amount\":20000,\"orderNo\":\"CY202010262257474833138\",\"statusDesc\":\"ad@admin.com手动补单,等待回调\",\"outOrderNo\":\"202010262257474055081\",\"errorMsg\":\"正常\",\"deleted\":false,\"merchantAccount\":\"亿万彩@merchant.com\",\"createTime\":1603724267000,\"notifyUrl\":\"http://pay.yzf.ink//notify\",\"merchantUserId\":161,\"payCode\":\"bankcard\",\"status\":\"SUCCESS\"}";
		PayOrder payOrder = JSON.parseObject(json, PayOrder.class);
		System.out.println(payOrder.getOrderNo());


//		String str="{\"convertClass\":\"com.ra.service.channel.BehalfInterface\",\"data\":{\"balance\":18316.00,\"bankBranch\":\"\",\"bankName\":\"中国工商银行\",\"bankNo\":\"6222021911005603292\",\"behalfGroupId\":17,\"behalfUserId\":145,\"comeIn\":true,\"comeOut\":true,\"createTime\":1603714225000,\"deleted\":false,\"id\":23,\"merchantFee\":1.00,\"merchantRate\":0.0070,\"proxyRate\":0.0030,\"realName\":\"肖都\",\"remark\":\"\"}}";
//		ExtraData extraData = JSON.parseObject(str, ExtraData.class);
//		System.out.println(extraData.getConvertClass());
	}
}

package com.ra.common.constant;

import java.math.BigDecimal;

/**
 * Redis keys 常量库
 * @author Administrator
 *
 */
public final class RedisConstant {

	/**
	 * 密码错误限制
	 */
	public final static String PASSWORDERRORLIMIT=RedisConstant.class.getName()+":password:errorLimit:";

	/**
	 * 支付密码错误限制
	 */
	public final static String PAY_PASSWORDERRORLIMIT=RedisConstant.class.getName()+":payPassword:errorLimit:";


	/**
	 * PDD订单锁
	 */
	public final static String ORDERINFO=RedisConstant.class.getName()+":order:info:";

	public static String getOrderInfo(String orderNo){
		return ORDERINFO+orderNo;
	}


}

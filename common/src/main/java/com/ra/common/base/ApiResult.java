package com.ra.common.base;

import com.alibaba.fastjson.JSONObject;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.HashMap;

/**
 * Rest api接口的通用返回结果格式
 * @author Administrator
 *
 */
public class ApiResult {

	/**
	 * Rest输出格式标准，统一方法
	 * @param code
	  * -200为未登录
	 * 1 为正常，标准结构，标准输出。
	 * -1为异常状态
	 * 大于0 其他正常状态
	 * 小于-1 其他异常状态
	 * -400 错误状态
	 * @param msg
	 * @param data
	 * @return
	 */
	@Getter
	private int code;
	@Getter
	private String msg;
	@Getter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Object data;
	/**
	 * 服务器时间
	 */
	@Getter
	private long serverTime=System.currentTimeMillis();
	/**
	 * Rest输出格式标准，统一方法
	  * -200为未登录
	 * 1 为正常，标准结构，标准输出。
	 * -1为异常状态
	 * 大于0 其他正常状态
	 * 小于-1 其他异常状态
	 * -400 错误状态
	 * @return
	 */
	public ApiResult(){
//		this(1, "success");
	}
	
	/**
	 * Rest输出格式标准，统一方法
	 * @param code
	  * -200为未登录
	 * 1为正常，标准结构，标准输出。
	 * -1为异常状态，比如逻辑要判断的，列如某个参数为空，就提示 code -1  msg 某参数不允许为空等等情况。
	 * 大于0 其他正常状态
	 * 小于-1 其他异常状态
	 * -400 错误状态
	 * @param msg
	 * @return
	 */
	public ApiResult(int code,String msg) {
		// TODO Auto-generated constructor stub
		this.code=code;
		this.msg=msg;
//		this(code,msg,null);
	}
//
//	/**
//	 * Rest输出格式标准，统一方法
//	 * @param code
//	  * -200为未登录
//	 * 1 为正常，标准结构，标准输出。
//	 * -1为异常状态
//	 * 大于0 其他正常状态
//	 * 小于-1 其他异常状态
//	 * -400 错误状态
//	 * @param msg
//	 * @param data
//	 * @return
//	 */
//	public ApiResult(int code,String msg,Object data) {
//		// TODO Auto-generated constructor stub
//		this.code=code;
//		this.msg=msg;
//		this.data=data;
//	}

	
	/**
	 * 失败code:-1 msg: fail
	 * @return
	 */
	public ApiResult fail(){
		this.code=-1;
		this.msg="fail";
		return this;
	}
	
	/**
	 * 失败code:-1 msg: fail
	 * @return
	 */
	public ApiResult fail(String msg){
		this.code=-1;
		this.msg=msg;
		return this;
	}

	/**
	 * 失败code:-1 msg: fail
	 * @return
	 */
	public ApiResult fail(int code,String msg){
		this.code=code;
		this.msg=msg;
		return this;
	}

	/**
	 * 成功code:1 msg: success
	 * @return
	 */
	public ApiResult ok(){
		this.code=1;
		this.msg="success";
		return this;
	}

	/**
	 * 成功code:1 msg: success
	 * @return
	 */
	public ApiResult ok(String msg){
		this.code=1;
		this.msg=msg;
		return this;
	}

	/**
	 * Object 赋值data
	 * @param data
	 * @return
	 */
	public ApiResult inject(Object data){
		this.data=data;
		return this;
	}

	public ApiResult put(String key,Object value){
		if(this.data==null){
			this.data=new HashMap<String,Object>();
		}

		if(this.data instanceof HashMap){
			HashMap<String,Object> map= (HashMap<String, Object>) this.data;
			map.put(key,value);
		}

		return this;
	}
	
	@Override
	public String toString() {
		return JSONObject.toJSONString(this);
	}
}

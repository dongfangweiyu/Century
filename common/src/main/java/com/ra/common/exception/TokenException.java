package com.ra.common.exception;

import com.ra.common.base.ApiResult;

/**
 * 异常处理器
 * @author Administrator
 * 该异常重写toString方法，适用于controller层返回数据异常时，直接toString从而返回一个json
 */
public class TokenException extends IllegalArgumentException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ApiResult apiResult;

	/**
	 * 异常处理器构造函数
	 * code 默认为 -1
	 * @param msg
	 */
	public TokenException(String msg) {
		// TODO Auto-generated constructor stub
		super(msg);
		this.apiResult=new ApiResult(-200, msg);
	}
	
	/**
	 * 异常处理器构造函数
	 * @param msg
	 */
	public TokenException(int code,String msg) {
		super(msg);
		// TODO Auto-generated constructor stub
		this.apiResult=new ApiResult(code, msg);
	}

	public ApiResult getApiResult() {
		return apiResult;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		//this.printStackTrace();
		return apiResult.toString();
	}
}

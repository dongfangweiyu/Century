package com.ra.common.exception;

import com.ra.common.base.ApiResult;

/**
 * 乐禾异常处理器
 * @author Administrator
 * 该异常重写toString方法，适用于controller层返回数据异常时，直接toString从而返回一个json
 */
public class ApiException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ApiResult apiResult;

	/**
	 * 乐禾异常处理器构造函数
	 * code 默认为 -1
	 * @param msg
	 */
	public ApiException(String msg) {
		// TODO Auto-generated constructor stub
		super(msg);
		this.apiResult=new ApiResult(-1, msg);
	}
	
	/**
	 * 乐禾异常处理器构造函数
	 * @param msg
	 */
	public ApiException(int code,String msg) {
		super(msg);
		// TODO Auto-generated constructor stub
		this.apiResult=new ApiResult(code, msg);
	}
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		//this.printStackTrace();
		return apiResult.toString();
	}
}

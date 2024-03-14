package com.ra.common.exception;

import com.ra.common.base.ApiResult;

/**
 * 系统配置异常处理器
 * @author Administrator
 * 该异常重写toString方法，适用于controller层返回数据异常时，直接toString从而返回一个json
 */
public class ConfigIllegalException extends IllegalArgumentException{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private ApiResult apiResult;

	private ConfigIllegalException(){
		super("setting config_system key or value is null");
		this.apiResult=new ApiResult(-1,"setting config_system key or value is null");
	}
	/**
	 * 异常处理器构造函数
	 * code 默认为 -1
	 * @param key
	 */
	public ConfigIllegalException(String key) {
		// TODO Auto-generated constructor stub
		super("the database table config_system is not exits key :"+key);
		this.apiResult=new ApiResult(-1, "the database table config_system is not exits key :"+key);
	}

	public ApiResult getApiResult() {
		return apiResult;
	}

	/**
	 * 过滤null值
	 * @param key
	 * @param value
	 */
	public static void filterKeyOrValue(String key,String value){
		if(key==null||value==null){
			throw new ConfigIllegalException();
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		//this.printStackTrace();
		return apiResult.toString();
	}
}

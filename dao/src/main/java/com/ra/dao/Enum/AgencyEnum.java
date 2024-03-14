package com.ra.dao.Enum;


import lombok.Getter;

/**
 * 机构类型枚举
 * @author Administrator
 *
 */
public enum AgencyEnum {
	ADMIN("管理员"),//管理员
	PROXY("业务员"),//业务员 代理
	MERCHANT("商户"),//商户
	BEHALF("代付商");

	@Getter
	private String desc;

	AgencyEnum(String desc){
		this.desc=desc;
	}
}

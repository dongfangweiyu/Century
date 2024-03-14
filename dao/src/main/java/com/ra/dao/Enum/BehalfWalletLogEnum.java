package com.ra.dao.Enum;


import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 机构类型枚举
 * @author Administrator
 *
 */
public enum BehalfWalletLogEnum {

	BEHALF("手动修改"),
	COMEIN("充值转入"),
	COMEOUT("代付支出");

	@Getter
	private String text;

	BehalfWalletLogEnum(String text){
		this.text=text;
	}


	private String description;

	public void setDescription(String description){
		this.description=description;
	}
	/**
	 * 获取描述
	 */
	public String  getDescription(){
		if(!StringUtils.isEmpty(this.description)) {
			return this.description;
		}
		return this.getText();
	}
}

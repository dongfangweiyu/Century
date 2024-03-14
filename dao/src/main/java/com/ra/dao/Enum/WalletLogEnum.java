package com.ra.dao.Enum;


import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 机构类型枚举
 * @author Administrator
 *
 */
public enum WalletLogEnum {

	ADMIN("管理员修改"),
	MERCHANT("商户订单成交收入"),
	BEHALF("卡商银行卡充值入账"),
	PROXY("代理分佣收入"),
	PROXYBEHALF("代理代付订单分佣收入"),
	BEHALFPROFIT("代付订单利润收入"),
	MERCHANTBEHALF("商户代付订单扣除余额"),
	MERCHANTBATCHBEHALF("商户批量代付订单扣除余额"),
	MERCHANTBEHALFCANCEL("代付订单取消"),
	MERCHANTWITHDRAWAL("提现支出"),
	WITHDRAWALRATE("提现手续费"),
	WITHDRAWALCANCEL("提现取消");

	@Getter
	private String text;

	WalletLogEnum(String text){
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
		switch (this){
			case ADMIN:
				return "管理员修改金额变动";
			case PROXY:
				return "代理分佣收入";
			case BEHALF:
				return "卡商银行卡充值入账";
			case MERCHANT:
				return "商户订单成交收入";
			case MERCHANTWITHDRAWAL:
				return "提现支出";
			case WITHDRAWALCANCEL:
				return "提现取消未交易金额转入钱包";
			default:
				return this.getText();
		}
	}
}

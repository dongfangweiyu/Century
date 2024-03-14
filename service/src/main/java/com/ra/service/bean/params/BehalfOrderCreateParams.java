package com.ra.service.bean.params;

import com.ra.dao.entity.business.BehalfBankCard;
import lombok.Data;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建OTC订单请求参数
 */
@Data
public class BehalfOrderCreateParams {

    @NotNull(message = "amount:订单金额不能为空")
    private BigDecimal amount;
    @NotBlank(message = "outOrderNo:商户订单号不能为空")
    private String outOrderNo;
    @NotNull(message = "timestamp:请求时间戳不能为空,且时间差不能大于60秒")
    private Long timestamp;
    /**
     * 业务流水号，不小于10位字符的随机字符串
     */
    @NotBlank(message = "nonceStr:业务流水号不小于10位字符随机字符串用于保证签名的不可预测性")
    private String nonceStr;

    @NotBlank(message = "appId:不能为空")
    private String appId;

    @NotBlank(message = "notifyUrl:异步回调通知地址不能为空")
    private String notifyUrl;

    @NotBlank(message = "signature:签名不能为空")
    private String signature;

    @NotNull(message = "银卡信息不能为空")
    private BehalfOrderCreateBankCardParams bankCard;

    //附加参数
    private String attach;
}

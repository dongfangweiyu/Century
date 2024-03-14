package com.ra.service.bean.req;

import lombok.Data;

@Data
public class ProxyIncomeLogReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String merchantAccount;//商户账号

    private Long payChannelId;//通道名称ID

    private String  payCode;//通道编码

    private Long incomeUserId;//收入的用户id

    private String incomeType;//收入类型
}

package com.ra.service.bean.req;

import lombok.Data;

@Data
public class MerchantQueryReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String queryParam;//账号或昵称

    private String proxyAccount;

    private Integer status;//状态
}

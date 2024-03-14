package com.ra.service.bean.req;

import lombok.Data;

@Data
public class OpenAccountReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String queryParam;//商户账号

    private String proxyAccount;//代理账号

    private String  applyStatus;//审核状态
}

package com.ra.service.bean.req;

import lombok.Data;

@Data
public class WalletLogReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String queryParam;//账号

    private String way;//变动方式

    private String logEnum;//变动类型
}

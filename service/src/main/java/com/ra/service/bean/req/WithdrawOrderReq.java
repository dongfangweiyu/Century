package com.ra.service.bean.req;

import lombok.Data;

@Data
public class WithdrawOrderReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private String status;//状态
}

package com.ra.service.bean.req;

import lombok.Data;

@Data
public class ChannelWalletLogReq {
    private String beginTime;//开始时间

    private String endTime;//结束时间

    private Long configPayInterfaceId;//配置ID

    private String way;//转入还是转出，  rollIn 转入，  rollOut转出
}

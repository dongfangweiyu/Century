package com.ra.service.bean.resp;

import lombok.Data;

import java.util.List;

@Data
public class BehalfBankCardGroupConfigBean {

    private Long behalfUserId;//卡商UserID
    private String behalfName;//卡商名称

    private List<CardGroupRateVo> cardGroupRateVoList;
}

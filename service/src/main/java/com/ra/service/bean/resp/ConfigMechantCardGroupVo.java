package com.ra.service.bean.resp;

import lombok.Data;

import java.util.List;

/**
 * 编辑商户卡组配置
 */
@Data
public class ConfigMechantCardGroupVo {

    private long userId;
    private List<CardGroupRateVo> cardGroupRateVoList;


}

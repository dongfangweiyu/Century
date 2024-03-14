package com.ra.service.bean.resp;

import lombok.Data;

import java.util.List;

/**
 * 批量下单数据
 */
@Data
public class BatchBehalfOrderDataVo {

    private List<BatchBehalfDataVo> allBatchBehalfData;


}

package com.ra.service.bean.resp;

import com.ra.dao.entity.business.PayChannel;
import lombok.Data;

import java.util.List;

@Data
public class PayCode2PayChannelBean {

    private Long payCodeId;
    private String payCodeName;

    private List<RateVo> rateVoList;
}

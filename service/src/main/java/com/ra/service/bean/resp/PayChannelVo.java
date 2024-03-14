package com.ra.service.bean.resp;

import com.ra.dao.entity.business.PayChannel;
import lombok.Data;

@Data
public class PayChannelVo extends PayChannel {

    //权重
    private Integer weight;
}

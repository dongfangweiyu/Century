package com.ra.service.bean.resp;

import lombok.Data;

import java.util.List;

@Data
public class ProxyAddInfoVo {

    private Long id;
    private String account;
    private String password;

    private List<RateVo> rateVoList;
}

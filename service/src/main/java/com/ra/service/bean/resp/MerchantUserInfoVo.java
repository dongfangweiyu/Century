package com.ra.service.bean.resp;

import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.security.User;
import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantUserInfoVo implements Serializable {

    public MerchantUserInfoVo(MerchantInfo merchantInfo,User user){
        this.merchantInfo=merchantInfo;
        this.user=user;
    }

    private MerchantInfo merchantInfo;

    private User user;
}

package com.ra.service.bean.resp;

import com.ra.dao.entity.business.BehalfInfo;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.security.User;
import lombok.Data;

import java.io.Serializable;

@Data
public class BehalfUserInfoVo implements Serializable {

    public BehalfUserInfoVo(BehalfInfo behalfInfo, User user){
        this.behalfInfo=behalfInfo;
        this.user=user;
    }

    private BehalfInfo behalfInfo;

    private User user;
}

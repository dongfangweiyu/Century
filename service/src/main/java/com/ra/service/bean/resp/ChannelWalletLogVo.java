package com.ra.service.bean.resp;

import com.ra.dao.entity.business.ChannelWalletLog;
import lombok.Data;

@Data
public class ChannelWalletLogVo extends ChannelWalletLog {
    private String configName;//账户
}

package com.ra.service.business;

import com.ra.dao.entity.business.BehalfBankCardGroup;
import com.ra.dao.repository.business.BehalfBankCardGroupRepository;
import com.ra.service.base.BaseService;

import java.util.List;

public interface BehalfBankCardGroupService extends BaseService<BehalfBankCardGroupRepository> {
    /**
     * 卡商自己的卡组列表
     */
    List<BehalfBankCardGroup> findBankCardGroupList(long userId,boolean enable);


    /**
     * 商户所绑定卡商下的卡组列表
     */
    List<BehalfBankCardGroup> findMerchantBankCardGroupList(long behalfUserId,long merchantUserId);

}

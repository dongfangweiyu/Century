package com.ra.service.business;

import com.ra.dao.entity.business.BehalfBankCardGroupRelation;
import com.ra.dao.repository.business.BehalfBankCardGroupRelationRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.resp.AddMechantInfoVo;
import com.ra.service.bean.resp.ConfigMechantCardGroupVo;

import java.util.List;

public interface BehalfBankCardGroupRelationService extends BaseService<BehalfBankCardGroupRelationRepository> {
    /**
     * 根据商户userId查询出  所配置的卡组配置
     * @param merchantUserId
     * @return
     */
    List<BehalfBankCardGroupRelation> findByBehalfBankCardGroupRelationList(long merchantUserId);

    /**
     * 编辑商户卡组配置
     * @param configMechantCardGroupVo
     * @return
     */
    boolean editMerchantConfigCardGroup(ConfigMechantCardGroupVo configMechantCardGroupVo);

    BehalfBankCardGroupRelation findBehalfBankCardGroupRelationInfo(Long merchantUserId,Long cardGroupId);
}

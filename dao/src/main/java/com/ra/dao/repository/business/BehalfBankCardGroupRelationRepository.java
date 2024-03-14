package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfBankCardGroupRelation;
import org.springframework.stereotype.Repository;


/**
 * 提现代付银行卡与商户的关系表Dao
 */
@Repository
public interface BehalfBankCardGroupRelationRepository extends BaseRepository<BehalfBankCardGroupRelation,Long> {
    BehalfBankCardGroupRelation findBehalfBankCardGroupRelationByBankCardGroupId(long bankCardGroupId);

}

package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfBankCardGroup;
import org.springframework.stereotype.Repository;


/**
 * 卡组Dao
 */
@Repository
public interface BehalfBankCardGroupRepository extends BaseRepository<BehalfBankCardGroup,Long> {
    BehalfBankCardGroup findById(long Id);

}

package com.ra.service.business.impl;

import com.ra.dao.entity.business.BehalfBankCardGroup;
import com.ra.dao.repository.business.BehalfBankCardGroupRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.business.BehalfBankCardGroupService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehalfBankCardGroupServiceImpl extends BaseServiceImpl<BehalfBankCardGroupRepository> implements BehalfBankCardGroupService {

    @Override
    public List<BehalfBankCardGroup> findBankCardGroupList(long userId,boolean enable) {
        Map<String,Object> params=new HashMap<>();
        params.put("behalfUserId",userId);
        String sql="SELECT b.* FROM business_behalf_bankCardGroup b where b.behalfUserId=:behalfUserId ";
        if (enable) {
            sql+=" and enable=:enable";
            params.put("enable",enable);
        }
        return findList(sql, params, BehalfBankCardGroup.class);
    }

    @Override
    public List<BehalfBankCardGroup> findMerchantBankCardGroupList(long behalfUserId, long merchantUserId) {
        Map<String,Object> params=new HashMap<>();
        params.put("behalfUserId",behalfUserId);
        params.put("merchantUserId",merchantUserId);
        String sql="SELECT b.* FROM business_behalf_bankCardGroup b left join business_behalf_bankCardGroupRelation r on b.id=r.bankCardGroupId \n" +
                "where b.behalfUserId=:behalfUserId and r.merchantUserId=:merchantUserId and enable=1 ";
        return findList(sql, params, BehalfBankCardGroup.class);
    }
}

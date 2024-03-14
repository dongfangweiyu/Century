package com.ra.service.business.impl;

import com.ra.dao.entity.business.BehalfBankCardGroupRelation;
import com.ra.dao.repository.business.BehalfBankCardGroupRelationRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.resp.CardGroupRateVo;
import com.ra.service.bean.resp.ConfigMechantCardGroupVo;
import com.ra.service.business.BehalfBankCardGroupRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehalfBankCardGroupRelationServiceImpl extends BaseServiceImpl<BehalfBankCardGroupRelationRepository> implements BehalfBankCardGroupRelationService {

    @Override
    public List<BehalfBankCardGroupRelation> findByBehalfBankCardGroupRelationList(long merchantUserId) {
        Map<String,Object> params=new HashMap<>();
        params.put("merchantUserId",merchantUserId);
        String sql="SELECT b.* FROM business_behalf_bankCardGroupRelation b where b.merchantUserId=:merchantUserId";
        return findList(sql, params, BehalfBankCardGroupRelation.class);
    }

    @Override
    @Transactional
    public boolean editMerchantConfigCardGroup(ConfigMechantCardGroupVo configMechantCardGroupVo) {
        List<BehalfBankCardGroupRelation> rateList=new ArrayList<>();
        List<BehalfBankCardGroupRelation> deleteList=new ArrayList<>();
        for(CardGroupRateVo rateVo:configMechantCardGroupVo.getCardGroupRateVoList()){
            BehalfBankCardGroupRelation rate=new BehalfBankCardGroupRelation();
            if(rateVo.isChecked()){
                //如果代理的费率大于商户的费率
                if(rateVo.getProxyRate().compareTo(rateVo.getMerchantRate())==1){
                    throw new IllegalArgumentException("代理的费率不能高于商户费率");
                }
                if(rateVo.getProxyFee().compareTo(rateVo.getMerchantFee())==1){
                    throw new IllegalArgumentException("代理的单笔不能高于商户单笔");
                }

                rate.setId(rateVo.getId());
                rate.setMerchantUserId(configMechantCardGroupVo.getUserId());
                rate.setBankCardGroupId(rateVo.getBankCardGroupId());
                rate.setMerchantRate(rateVo.getMerchantRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setProxyRate(rateVo.getProxyRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setMerchantFee(rateVo.getMerchantFee());
                rate.setProxyFee(rateVo.getProxyFee());
                rateList.add(rate);
            }else if(rateVo.getId()!=null&&rateVo.getId()>0){
                rate.setId(rateVo.getId());
                deleteList.add(rate);
            }
        }
        repository.saveAll(rateList);
        repository.deleteAll(deleteList);
        return true;
    }

    @Override
    public BehalfBankCardGroupRelation findBehalfBankCardGroupRelationInfo(Long merchantUserId,Long cardGroupId) {
        Map<String,Object> params=new HashMap<>();
        params.put("merchantUserId",merchantUserId);
        params.put("cardGroupId",cardGroupId);
        String sql="SELECT b.* FROM business_behalf_bankCardGroupRelation b where b.bankCardGroupId=:cardGroupId and merchantUserId= :merchantUserId";
        return findOne(sql, params, BehalfBankCardGroupRelation.class);
    }
}

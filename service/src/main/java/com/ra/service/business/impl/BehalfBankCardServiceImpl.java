package com.ra.service.business.impl;

import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.entity.business.BehalfBankCardGroup;
import com.ra.dao.repository.business.BehalfBankCardRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.resp.BehalfBankCardResp;
import com.ra.service.business.BehalfBankCardService;
import com.ra.service.business.BehalfBankCardWalletLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehalfBankCardServiceImpl extends BaseServiceImpl<BehalfBankCardRepository> implements BehalfBankCardService {
    @Autowired
    BehalfBankCardWalletLogService behalfBankCardWalletLogService;
    @Override
    public BehalfBankCardResp matchBehalfBankCard(long merchantUserId, boolean comeIn) {
        Map<String,Object> params=new HashMap<>();
        params.put("merchantUserId",merchantUserId);
        String sql="select b.*,r.merchantRate,r.proxyRate,r.merchantFee from business_behalf_bankcard as b LEFT JOIN business_behalf_bankcardgrouprelation as r on r.bankCardGroupId=b.behalfGroupId LEFT JOIN business_behalf_bankcardgroup as g on g.id=r.bankCardGroupId WHERE r.merchantUserId=:merchantUserId and g.`enable`=1 ";
        if(comeIn){
            sql+="  and b.comeIn=1 ";
        }else{
            sql+="  and b.comeOut=1 ";
        }
        sql+=" ORDER BY RAND() ";
        return findOne(sql,params,BehalfBankCardResp.class);
    }

    @Override
    public List<BehalfBankCard> findListBehalfBankCard(long bankCardGroupId) {
        Map<String,Object> params=new HashMap<>();
        params.put("bankCardGroupId",bankCardGroupId);
        String sql="SELECT b.* FROM business_behalf_bankCard b where b.behalfGroupId=:bankCardGroupId";
        return findList(sql, params, BehalfBankCard.class);
    }

    @Override
    public int addMoney(long cardId, BigDecimal amount, BehalfWalletLogEnum behalfWalletLogEnum) {
        boolean b = behalfBankCardWalletLogService.saveBehalfBankCardLog(cardId, amount, behalfWalletLogEnum);
        if(b){
            int i = getRepository().addMoney(amount, cardId);
            if(i>0){
                return i;
            }
        }
        throw new IllegalArgumentException("增加金额失败");
    }

    @Override
    public int subMoney(long cardId, BigDecimal amount, BehalfWalletLogEnum behalfWalletLogEnum) {
        boolean b= behalfBankCardWalletLogService.saveBehalfBankCardLog(cardId,BigDecimal.ZERO.subtract(amount),behalfWalletLogEnum);
        if(b){
            int i = getRepository().subMoney(amount,cardId);
            if(i>0){
                return i;
            }
        }
        throw new IllegalArgumentException("减少金额失败");
    }
}

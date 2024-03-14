package com.ra.service.business;

import com.ra.dao.Enum.BehalfWalletLogEnum;
import com.ra.dao.entity.business.BehalfBankCard;
import com.ra.dao.repository.business.BehalfBankCardRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.resp.BehalfBankCardResp;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface BehalfBankCardService extends BaseService<BehalfBankCardRepository> {


    /**
     * 匹配一张代付银行卡
     * @param merchantUserId 商户userId
     * @param comeIn 是否转入，true转入，false转出
     * @return
     */
    BehalfBankCardResp matchBehalfBankCard(long merchantUserId, boolean comeIn);

    /**
     * 所属卡组下的代付银行卡列表
     * @param bankCardGroupId
     * @return
     */
    List<BehalfBankCard> findListBehalfBankCard(long bankCardGroupId);

    /**
     * 根据cardId获取银行卡信息
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int addMoney(long cardId, BigDecimal amount, BehalfWalletLogEnum behalfWalletLogEnum);

    /**
     * 根据cardId获取银行卡信息
     * 保存金额变动日记
     * @return
     */
    @Transactional
    int subMoney(long cardId, BigDecimal amount,BehalfWalletLogEnum behalfWalletLogEnum);
}

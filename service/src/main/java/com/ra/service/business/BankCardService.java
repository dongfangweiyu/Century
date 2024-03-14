package com.ra.service.business;

import com.ra.dao.entity.business.BankCard;
import com.ra.dao.repository.business.BankCardRepository;
import com.ra.service.base.BaseService;

import java.util.List;

public interface BankCardService extends BaseService<BankCardRepository> {

    /**
     * 根据userId查看自己的收款账户列表
     * @param userId
     * @return
     */
    List<BankCard> findBankCardList(long userId);
}

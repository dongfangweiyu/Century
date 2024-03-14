package com.ra.service.business.impl;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BankCard;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.repository.business.BankCardRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.business.BankCardService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankCardServiceImpl extends BaseServiceImpl<BankCardRepository> implements BankCardService {


    @Override
    public List<BankCard> findBankCardList(long userId) {
        Map<String,Object> params=new HashMap<>();
        params.put("userId",userId);
        String sql="select * from business_bankCard where userId=:userId";
        return findList(sql, params, BankCard.class);
    }
}

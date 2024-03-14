package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.PayCode;
import com.ra.dao.entity.business.PayCode2Channel;
import org.springframework.stereotype.Repository;

@Repository
public interface PayCode2ChannelRepository extends BaseRepository<PayCode2Channel,Long> {


    void deleteByPayChannelId(long payChannelId);
}
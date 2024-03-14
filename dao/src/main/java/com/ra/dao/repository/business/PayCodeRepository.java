package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.business.PayCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayCodeRepository extends BaseRepository<PayCode,Long> {

    PayCode findById(long id);

    PayCode findByCode(String code);

}
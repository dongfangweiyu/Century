package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 支付类型与通道的映射表
 */
@Data
@Entity
@Table(name="business_payCode2Channel")
public class PayCode2Channel extends BaseEntity {


    private Long payCodeId;

    private Long payChannelId;

}

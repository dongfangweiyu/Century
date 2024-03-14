package com.ra.dao.entity.business;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name="business_bankList")
public class BankList extends BaseEntity {

    /**
     * 银行名称
     * 建设银行
     */
    private String bankName;
}

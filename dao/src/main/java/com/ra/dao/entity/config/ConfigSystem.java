package com.ra.dao.entity.config;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 系统配置
 * key,value方式存储
 */
@Data
@Entity
@Table(name="config_system")
public class ConfigSystem extends BaseEntity {

    @Column(name = "cKey", columnDefinition = "varchar(255) comment '配置项key'",unique = true,nullable = false)
    private String cKey;

    @Lob
    @Column(columnDefinition="TEXT")
    private String cValue;


    private String description;
}

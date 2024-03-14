package com.ra.dao.entity.security;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 路径
 */
@Entity
@Table(name = "security_page")
@Data
public class Page extends BaseEntity {

    private static final long serialVersionUID = -7905164377443087163L;

    @Column(name = "name", columnDefinition = "varchar(50) comment '名称'")
    private String name;

    @Column(name = "href", columnDefinition = "varchar(255) comment '引用'")
    private String href;

}

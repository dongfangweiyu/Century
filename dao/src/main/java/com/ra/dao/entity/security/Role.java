package com.ra.dao.entity.security;

import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 角色表
 * 1 admin
 * 2 运营人员
 * 3 产品
 * 4 测试
 *
 * @author huli
 */
@Entity
@Table(name = "security_role")
@Data
public class Role extends BaseEntity {

    private static final long serialVersionUID = -281431228808345995L;

    @Column(name = "name", columnDefinition = "varchar(255) comment '名称'")
    private String name;

    @Column(name = "description", columnDefinition = "varchar(255) comment '描述'")
    private String description;

    @Column(name = "status", columnDefinition = "int(5) comment '状态 0 启用 1 禁用'")
    private Integer status;

}

package com.ra.dao.entity.security;

import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.base.BaseEntity;
import lombok.Data;

import javax.persistence.*;

/**
 * @author huli
 */
@Entity
@Table(name = "security_nav")
@Data
public class Navigation extends BaseEntity {

    private static final long serialVersionUID = 3378521096681428950L;

    /**
     * 标题
     */
    @Column(name = "title", columnDefinition = "varchar(50) COMMENT '导航标题'")
    private String title;

    /**
     * 图标
     */
    @Column(name = "icon", columnDefinition = "varchar(255) COMMENT '图标'")
    private String icon;

    /**
     * 所属页面
     */
    @Column(name = "pageId", columnDefinition = "int(200) COMMENT '所属页面'")
    private Long pageId;

    /**
     * 父目录
     */
    @Column(name = "parentId", columnDefinition = "int(50) comment '父目录'")
    private Long parentId;


    /**
     * 所属机构
     */
    @Column(name = "agencyEnum",columnDefinition = "varchar(255) comment '所属机构'",nullable = false)
    @Enumerated(EnumType.STRING)
    private AgencyEnum agencyEnum;

    /**
     * 所属角色
     */
    @Column(name = "roleId", columnDefinition = "varchar(255) comment '所属角色'")
    private Long roleId;

}

package com.ra.service.bean.resp;

import com.ra.dao.entity.security.User;
import lombok.Data;

@Data
public class UserAdminVo extends User {

    private String roleName;

}

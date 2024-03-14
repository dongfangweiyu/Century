package com.ra.dao.repository.security;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.security.Role;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-04
 * @description
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {

    /**
     * 通过id查询
     * @param id
     * @return
     */
    Role findRoleById(Long id);

    /**
     * 更新角色
     * @param name
     * @param description
     * @param id
     * @return
     */
    @Modifying
    @Query(value = "update from security_role set `name` = :name, description = :description where id = :id",
            nativeQuery = true)
    Role updateRole(@Param("name") String name, @Param("description") String description, @Param("id") Long id);

    @Modifying
    @Query(value = "update from security_role set authoritys = :auths where id = :id", nativeQuery = true)
    Role addRolleWihtAuths(@Param("auths") String auths, @Param("id") Long id);


    @Modifying
    @Query(value = "select * from security_role where status = 0", nativeQuery = true)
    List<Role> findRoles();

}

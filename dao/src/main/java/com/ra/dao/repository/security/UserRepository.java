package com.ra.dao.repository.security;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.security.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author huli
 * @time 2019-01-02
 * @description
 */
@Repository
public interface UserRepository extends BaseRepository<User,Long> {

    /**
     * 通过id查询用户
     *
     * @param id 主键
     * @return 用户
     */
    @Query("from User where id = :id")
    User findUserById(Long id);

    /**
     * 通过账号account查询用户
     * @param account
     * @return
     */
    User findUserByAccount(String account);

    /**
     * 通过id更新状态
     * @param id 主键
     * @param status 状态代码 0 正常 1 禁用
     * @return 用户
     */
    @Modifying
    @Query(value = "update security_user set status = :status where id = :id", nativeQuery = true)
    @Transactional(rollbackFor = Exception.class)
    Integer updateStatus(@Param("id") Long id, @Param("status") Integer status);


    /**
     * 通过用户名和密码查询用户
     *
     * @param account
     * @param password
     * @return
     */
    User findUserByAccountAndPassword(String account, String password);


    /**
     * 通过角色id不为空筛选用户
     * @return
     */
    List<User> findUserByRoleIDNotNull();
    /**
     * 修改备注
     */
    @Modifying
    @Transactional
    @Query(value = "update security_user set remark=:description where id=:userId ",nativeQuery = true)
    int updateUserRemark(@Param("userId") long userId,@Param("description") String description);


    /**
     * 解绑谷歌验证
     */
    @Modifying
    @Transactional
    @Query(value = "update security_user set googleAuthenticator=null where id=:userId ",nativeQuery = true)
    int updateUsergoogle(@Param("userId") long userId);
}

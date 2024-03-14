package com.ra.service.base;

import com.ra.dao.base.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

public interface BaseService<K extends  BaseRepository> {

    /**
     * 获取BaseRepository对象
     * @return
     */
     K getRepository();

    /**
     * 获取EntityManager对象
     * @return
     */
     EntityManager getEntityManager();
    /**
     * 原生sql查询单条
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param cls java bean
     * @param <T> 返回bean
     * @return
     */
     <T> T findOne(String sql, Map<String, Object> params, Class<T> cls);

    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param cls java bean
     * @param <T> 返回bean
     * @return
     */
     <T> List<T> findList(String sql, Map<String, Object> params, Class<T> cls);

    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回Map<String,Object>
     * @return
     */
     List<Map<String,Object>> findList(String sql, Map<String, Object> params);

    /**
     * 原生sql查询总条数COUNT
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回 int
     * @return
     */
    public int findCount(String sql, Map<String, Object> params);

    /**
     * 原生sql查询总条数COUNT
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回 int
     * @return
     */
    public int execute(String sql, Map<String, Object> params);
    /**
     * 原生sql查询累和SUM
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回 double
     * @return
     */
    public double findSum(String sql, Map<String, Object> params);

    /**
     * 原生sql查询分页，sql中不允许有limit关键字
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 查询参数,key->value
     * @param pageRequest 分页请求
     * @return
     */
    Page<Map<String,Object>> findPage(String sql,Map<String, Object> params, PageRequest pageRequest);


        /**
         * 原生sql查询分页，sql中不允许有limit关键字
         * @param sql 参数用匿名占位符，不用？占位符
         * @param pageRequest 分页请求
         * @param cls java bean
         * @param <T> 返回bean
         * @return
         */
     <T>  Page<T> findPage(String sql,Map<String, Object> params, Class<T> cls, PageRequest pageRequest);


}

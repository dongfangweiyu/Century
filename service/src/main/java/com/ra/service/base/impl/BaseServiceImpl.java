package com.ra.service.base.impl;

import com.alibaba.fastjson.JSON;
import com.ra.common.domain.PagerImpl;
import com.ra.dao.base.BaseRepository;
import com.ra.service.base.BaseService;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

/**
 * 基类Servuce
 * 用@Qualifier注解，优先级最高
 * @param <K>
 */
@NoRepositoryBean
public class BaseServiceImpl<K extends BaseRepository> implements BaseService<K> {

    @Autowired
    protected K repository;

    @Autowired
    protected EntityManager entityManager;

    @Override
    public K getRepository() {
        return repository;
    }

    @Override
    public EntityManager getEntityManager(){
        return entityManager;
    }
    /**
     * 原生sql查询单条
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param cls java bean
     * @param <T> 返回bean
     * @return
     */
    @Override
    public <T> T findOne(String sql, Map<String, Object> params, Class<T> cls) {
        List<Map<String, Object>> maps = findList(sql, params, null, 1);
        if(maps!=null&&maps.size()>0){
            return JSON.parseObject(JSON.toJSONString(maps.get(0)),cls);
        }
        return null;
    }

    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param cls java bean
     * @param <T> 返回bean
     * @return
     */
    public <T> List<T> findList(String sql, Map<String, Object> params, Class<T> cls) {
        List<Map<String, Object>> maps = findList(sql, params);
        return JSON.parseArray(JSON.toJSONString(maps),cls);
    }

    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回Map<String,Object>
     * @return
     */
    public List<Map<String,Object>> findList(String sql, Map<String, Object> params) {
        return findList(sql,params,null,null);
    }


    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回Map<String,Object>
     * @return
     */
    public List<Map<String,Object>> findList(String sql, Map<String, Object> params,Integer limit) {
        return findList(sql,params,null,limit);
    }

    /**
     * 原生sql查询列表
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回Map<String,Object>
     * @return
     */
    public List<Map<String,Object>> findList(String sql, Map<String, Object> params,Integer first,Integer limit) {
        Query nativeQuery = entityManager.createNativeQuery(sql);
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                nativeQuery.setParameter(key, params.get(key));
            }
        }
        org.hibernate.Query query = nativeQuery.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        if(first!=null){
            query.setFirstResult(first);
        }
        if(limit!=null){
            query.setMaxResults(limit);
        }
        return query.list();
    }

    /**
     * 原生sql查询总条数COUNT
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回 int
     * @return
     */
    @Override
    public int findCount(String sql, Map<String, Object> params) {
        org.hibernate.Query nativeQuery= entityManager.createNativeQuery(sql).unwrap(SQLQuery.class);
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                nativeQuery.setParameter(key, params.get(key));
            }
        }
        return Integer.parseInt(nativeQuery.uniqueResult().toString());
    }

    @Transactional
    @Modifying
    @Override
    public int execute(String sql, Map<String, Object> params) {
        org.hibernate.Query nativeQuery= entityManager.createNativeQuery(sql).unwrap(SQLQuery.class);
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                nativeQuery.setParameter(key, params.get(key));
            }
        }
        return nativeQuery.executeUpdate();
    }

    /**
     * 原生sql查询累和SUM
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 匿名参数Map
     * @param <> 返回 int
     * @return
     */
    public double findSum(String sql, Map<String, Object> params) {
        org.hibernate.Query nativeQuery= entityManager.createNativeQuery(sql).unwrap(SQLQuery.class);
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                nativeQuery.setParameter(key, params.get(key));
            }
        }
        return Double.parseDouble(nativeQuery.uniqueResult().toString());
    }

    /**
     * 原生sql查询分页，sql中不允许有limit关键字
     * @param sql 参数用匿名占位符，不用？占位符
     * @param params 查询参数,key->value
     * @param pageRequest 分页请求
     * @return
     */
    public  Page<Map<String,Object>> findPage(String sql,Map<String, Object> params, PageRequest pageRequest) {

        //先查列表
        List<Map<String, Object>> maps = findList(sql, params, pageRequest.getPageNumber() * pageRequest.getPageSize(), pageRequest.getPageSize());
        //再查总数
        int total = findCount(Helper.newCountQuery(sql), params);
        return new PagerImpl(maps,pageRequest,total);
    }


    /**
     * 原生sql查询分页，sql中不允许有limit关键字
     * @param sql 参数用匿名占位符，不用？占位符
     * @param pageRequest 分页请求
     * @param cls java bean
     * @param <T> 返回bean
     * @return
     */
    @Override
    public <T>  Page<T> findPage(String sql, Map<String, Object> params, Class<T> cls, PageRequest pageRequest) {

        //先查列表
        List<Map<String, Object>> maps = findList(sql, params, pageRequest.getPageNumber() * pageRequest.getPageSize(), pageRequest.getPageSize());
        List<T> list = JSON.parseArray(JSON.toJSONString(maps), cls);

        //再查总数
        int total = findCount(Helper.newCountQuery(sql), params);
        return new PagerImpl(list,pageRequest,total);
    }


    /**
     * sql语句处理辅助内部类
     */
    private static class Helper {
        private static final String DISTINCT_PATTERN = "(?i)select\\s+distinct\\s(.+)\\sfrom\\s+.+";

        static String newCountQuery(String queryString) {
            return isQueryContainsDistinct(queryString) ? newDistinctCountQuery(queryString) : newCommonsCountQuery(queryString);
        }

        private static boolean isQueryContainsDistinct(String queryString) {
            return queryString.matches(DISTINCT_PATTERN);
        }

        private static String newDistinctCountQuery(String queryString) {
            String items = getDistinctItems(queryString);
            String fromQuery = getFromQuery(queryString);
            return String.format("select count(distinct %s) %s", items, fromQuery);
        }

        private static String getFromQuery(String queryString) {
            String fromQuery = queryString;
            if (fromQuery.indexOf(" from") != -1) {
                fromQuery = fromQuery.substring(fromQuery.indexOf(" from"));
            }
            if (fromQuery.indexOf(" FROM") != -1) {
                fromQuery = fromQuery.substring(fromQuery.indexOf(" FROM"));
            }
            return fromQuery;
        }

        private static String getDistinctItems(String queryString) {
            return queryString.replaceAll(DISTINCT_PATTERN, "$1");
        }

        private static String newCommonsCountQuery(String queryString) {
            String fromSQL = getFromQuery(queryString);
            return String.format("select count(*) %s", fromSQL);
        }
    }
}

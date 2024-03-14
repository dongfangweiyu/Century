package com.ra.dao.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Iterator;

@NoRepositoryBean
public class BaseRepositoryImpl<T extends BaseEntity,ID> extends SimpleJpaRepository<T,ID> implements BaseRepository<T,ID>{
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager em;

    @Autowired
    public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityInformation=JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        this.em = entityManager;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicDelete(ID id) {
        Assert.notNull(id, "The given id must not be null!");
            String sql=String.format("update %s set deleted=1 where id=:id",this.entityInformation.getEntityName());
            this.em.createQuery(sql).setParameter("id",id).executeUpdate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicDelete(Iterable<ID> ids) {
        Assert.notNull(ids, "The given Iterable of ids not be null!");
        if (ids.iterator().hasNext()) {
            String queryString = QueryUtils.getQueryString("update %s x set deleted=1 where ", this.entityInformation.getEntityName());
            StringBuilder builder = new StringBuilder(queryString);
            Iterator<ID> iterator = ids.iterator();
            int i=1;
            while (iterator.hasNext()){
                iterator.next();
                builder.append(String.format(" id=?%d ",i));
                if(iterator.hasNext()){
                    builder.append(" or ");
                }
                i++;
            }
            Query query = this.em.createQuery(builder.toString());
            i=0;
            iterator=ids.iterator();
            while(iterator.hasNext()){
                ++i;
                query.setParameter(i,iterator.next());
            }
            query.executeUpdate();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicBatchDelete(Iterable<T> entities) {
        Assert.notNull(entities, "The given Iterable of entities not be null!");
        if (entities.iterator().hasNext()) {
            QueryUtils.applyAndBind(QueryUtils.getQueryString("update from %s x  set deleted=1 ", this.entityInformation.getEntityName()), entities, this.em).executeUpdate();
        }
    }


}

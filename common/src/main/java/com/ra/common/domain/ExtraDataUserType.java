package com.ra.common.domain;

import com.alibaba.fastjson.JSON;
import com.ra.common.bean.ExtraData;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class ExtraDataUserType implements UserType,Serializable {
    /**
     * Return the SQL type codes for the columns mapped by this type. The
     * codes are defined on <tt>java.sql.Types</tt>.
     *
     * @return int[] the typecodes
     * @see java.sql.Types
     */
    @Override
    public int[] sqlTypes() {
        return new int[] { Types.JAVA_OBJECT};
    }

    /**
     * The class returned by <tt>nullSafeGet()</tt>.
     *
     * @return Class
     */
    @Override
    public Class returnedClass() {
        return ExtraData.class;
    }

    /**
     * Compare two instances of the class mapped by this type for persistence "equality".
     * Equality of the persistent state.
     *
     * @param x
     * @param y
     * @return boolean
     */
    @Override
    public boolean equals(Object x, Object y) throws HibernateException {

        if( x== null){

            return y== null;
        }

        return x.equals( y);
    }

    /**
     * Get a hashcode for the instance, consistent with persistence "equality"
     */
    @Override
    public int hashCode(Object x) throws HibernateException {

        return x.hashCode();
    }

    /**
     * Retrieve an instance of the mapped class from a JDBC resultset. Implementors
     * should handle possibility of null values.
     *
     * @param resultSet      a JDBC result set
     * @param strings   the column names
     * @param sharedSessionContractImplementor
     * @param o   the containing entity  @return Object
     * @throws org.hibernate.HibernateException
     *
     * @throws java.sql.SQLException
     */
    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] strings, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException, SQLException {
        if(resultSet.getString(strings[0]) == null){
            return null;
        }
        return JSON.parseObject(resultSet.getString(strings[0]), returnedClass());
    }

    /**
     * Write an instance of the mapped class to a prepared statement. Implementors
     * should handle possibility of null values. A multi-column type should be written
     * to parameters starting from <tt>index</tt>.
     *
     * @param preparedStatement      a JDBC prepared statement
     * @param o   the object to write
     * @param i   statement parameter index
     * @param sharedSessionContractImplementor
     * @throws org.hibernate.HibernateException
     *
     * @throws java.sql.SQLException
     */
    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object o, int i, SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException, SQLException {
        if (o == null) {
            preparedStatement.setNull(i, Types.OTHER);
            return;
        }

        preparedStatement.setObject(i, JSON.toJSONString(o), Types.OTHER);
    }

    /**
     * Return a deep copy of the persistent state, stopping at entities and at
     * collections. It is not necessary to copy immutable objects, or null
     * values, in which case it is safe to simply return the argument.
     *
     * @param value the object to be cloned, which may be null
     * @return Object a copy
     */
    @Override
    public Object deepCopy(Object value) throws HibernateException {

        return value;
    }

    /**
     * Are objects of this type mutable?
     *
     * @return boolean
     */
    @Override
    public boolean isMutable() {
        return true;
    }

    /**
     * Transform the object into its cacheable representation. At the very least this
     * method should perform a deep copy if the type is mutable. That may not be enough
     * for some implementations, however; for example, associations must be cached as
     * identifier values. (optional operation)
     *
     * @param value the object to be cached
     * @return a cachable representation of the object
     * @throws org.hibernate.HibernateException
     *
     */
    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (String)this.deepCopy( value);
    }

    /**
     * Reconstruct an object from the cacheable representation. At the very least this
     * method should perform a deep copy if the type is mutable. (optional operation)
     *
     * @param cached the object to be cached
     * @param owner  the owner of the cached object
     * @return a reconstructed object from the cachable representation
     * @throws org.hibernate.HibernateException
     *
     */
    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return this.deepCopy( cached);
    }

    /**
     * During merge, replace the existing (target) value in the entity we are merging to
     * with a new (original) value from the detached entity we are merging. For immutable
     * objects, or null values, it is safe to simply return the first parameter. For
     * mutable objects, it is safe to return a copy of the first parameter. For objects
     * with component values, it might make sense to recursively replace component values.
     *
     * @param original the value from the detached entity being merged
     * @param target   the value in the managed entity
     * @return the value to be merged
     */
    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}

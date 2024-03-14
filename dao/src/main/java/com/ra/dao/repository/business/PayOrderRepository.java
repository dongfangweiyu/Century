package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.PayOrder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PayOrderRepository extends BaseRepository<PayOrder,Long> {

    /**
     * 通过订单号查询订单
     * @param orderNo
     * @return
     */
    PayOrder findByOrderNo(String orderNo);

    /**
     * 更新订单状态描述
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set statusDesc=:statusDesc where id=:id ",nativeQuery = true)
    int updateStatusDesc(@Param("id") long id, @Param("statusDesc") String statusDesc);


    /**
     * 更新订单状态描述
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set extraData=:extraData where id=:id and extraData is null ",nativeQuery = true)
    int updateExtraData(@Param("id") long id, @Param("extraData") String extraData);

    /**
     * 更新订单第三方返回的错误信息
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set errorMsg=:errorMsg where id=:id ",nativeQuery = true)
    int updateErrorMsg(@Param("id") long id, @Param("errorMsg") String errorMsg);

    /**
     * 回调订单次数++
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set notifyCount=notifyCount+1 where id=:id and status = 'SUCCESS' ",nativeQuery = true)
    int notifyOrderCount(@Param("id") long id);


    /**
     * 成功订单
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set status='SUCCESS',successTime=now(),statusDesc=:statusDesc where id=:id and status = 'PROCESS' ",nativeQuery = true)
    int successOrder(@Param("id") long id,@Param("statusDesc") String statusDesc);


    /**
     * 回调订单
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set completeTime=now(),statusDesc=:statusDesc where id=:id and status = 'SUCCESS' and completeTime is null ",nativeQuery = true)
    int completeOrder(@Param("id") long id,@Param("statusDesc") String statusDesc);

    /**
     * 定时器，批量关闭订单
     * 关闭30分钟之前为完成支付的订单
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_pay set closeTime=now(),status='FAIL',statusDesc='超时未支付,系统自动关闭订单' where status = 'PROCESS' and createTime < DATE_SUB(NOW(), INTERVAL 180 MINUTE)  ",nativeQuery = true)
    int closeOrderBatch();
}
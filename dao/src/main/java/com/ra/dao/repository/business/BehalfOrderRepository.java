package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.BehalfOrder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 代发提现订单Dao
 */
@Repository
public interface BehalfOrderRepository extends BaseRepository<BehalfOrder,Long> {

    BehalfOrder findByOrderNo(String orderNo);

    BehalfOrder findBehalfOrderById(long Id);
    /**
     * 更新订单状态描述
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set statusDesc=:statusDesc where id=:id ",nativeQuery = true)
    int updateStatusDesc(@Param("id") long id, @Param("statusDesc") String statusDesc);

    /**
     * 回调订单次数++
     * @param id
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set notifyCount=notifyCount+1 where id=:id and status != 'PROCESS' ",nativeQuery = true)
    int notifyOrderCount(@Param("id") long id);


    /**
     * 回调订单
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set completeTime=now(),statusDesc=:statusDesc where id=:id and status != 'PROCESS' and completeTime is null  ",nativeQuery = true)
    int completeOrder(@Param("id") long id,@Param("statusDesc") String statusDesc);

    /**
     * 取消该代付提现订单
     * @param behalfOrderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set status='FAIL',closeTime=now(),statusDesc=:statusDesc,remark=:remark where id=:behalfOrderId and status='PROCESS' ",nativeQuery = true)
    int cannelBehalfOrder(@Param("behalfOrderId") long behalfOrderId,@Param("statusDesc")String statusDesc,@Param("remark") String remark);

    /**
     * 确认付款上传凭证
     * @param behalfOrderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set paymentVoucher=:paymentVoucher,statusDesc=:statusDesc where id=:behalfOrderId and status='PROCESS' and paymentVoucher is null ",nativeQuery = true)
    int confirmBehalfOrder(@Param("behalfOrderId") long behalfOrderId,@Param("paymentVoucher") String paymentVoucher,@Param("statusDesc") String statusDesc);

    /**
     * 成功订单
     * @param orderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_behalf_order set status='SUCCESS',successTime=now(),statusDesc=:statusDesc,dealAccount=:dealAccount where id=:orderId and status='PROCESS' and dealAccount is null",nativeQuery = true)
    int successOrder(@Param("orderId") long orderId,@Param("statusDesc") String statusDesc,@Param("dealAccount") String dealAccount);
}

package com.ra.dao.repository.business;

import com.ra.dao.base.BaseRepository;
import com.ra.dao.entity.business.WithdrawlOrder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WithdrawOrderRepository extends BaseRepository<WithdrawlOrder,Long> {

    WithdrawlOrder findWithdrawlOrderById(long id);


    /**
     * 我处理
     * @param withdrawOrderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_withdraw set dealAccount=:dealAccount where id=:withdrawOrderId and status='PROCESS' and dealAccount is null ",nativeQuery = true)
    int meDealWithdarwOrder(@Param("withdrawOrderId") long withdrawOrderId,@Param("dealAccount") String dealAccount);
    /**
     * 取消该提现订单
     * @param withdrawOrderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_withdraw set status='FAIL',closeTime=now(),remark=:remark where id=:withdrawOrderId and status='PROCESS' ",nativeQuery = true)
    int cannelWithdarwOrder(@Param("withdrawOrderId") long withdrawOrderId,@Param("remark") String remark);

    /**
     * 确认付款上传凭证
     * @param orderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_withdraw set paymentVoucher=:paymentVoucher,remark=:remark where id=:orderId and status='PROCESS' and paymentVoucher is null ",nativeQuery = true)
    int confirmOrder(@Param("orderId") long orderId,@Param("paymentVoucher") String paymentVoucher,@Param("remark") String remark);

    /**
     * 成功订单
     * @param orderId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update business_order_withdraw set status='SUCCESS',closeTime=now() where id=:orderId and status='PROCESS' and paymentVoucher is not null ",nativeQuery = true)
    int successOrder(@Param("orderId") long orderId);
}

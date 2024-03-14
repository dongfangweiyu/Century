package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.WithdrawlOrder;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.business.WithdrawOrderRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.WithdrawOrderVo;
import org.springframework.data.domain.Page;

public interface WithdrawOrderService extends BaseService<WithdrawOrderRepository> {

    /**
     *根据userID查看自己的提现订单(如果传userId为Null则查询所有提现订单列表)
     * @return
     */
    Page<WithdrawOrderVo> findWithdrawOrderList(WithdrawOrderReq withdrawOrderReq, Long userId, Pager pager);

    /**
     * 发起提现
     * @param withdrawlOrder
     * @return
     */
    boolean addWithdrawal(WithdrawlOrder withdrawlOrder);

    /**
     * 我处理
     * @param orderId
     * @param dealAccount
     * @return
     */
    boolean meDealOrder(long orderId,String dealAccount);
    /**
     * 确认付款订单
     * @param orderId
     * @return
     */
    boolean confirmOrder(long orderId, String paymentVoucher, String remark);

    /**
     * 完结该提现订单
     * @return
     */
    boolean compeleteOrder(long orderId,long userId);

    /**
     * 取消订单
     * @param withdrawOrderId
     * @param userId
     * @return
     */
    boolean cancelWithdrawOrder(long withdrawOrderId,long userId,String remark);
}


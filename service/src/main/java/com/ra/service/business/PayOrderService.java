package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.repository.business.PayOrderRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.params.CreateOrderResp;
import com.ra.service.bean.params.OrderCreateParams;
import com.ra.service.bean.params.OrderQueryParams;
import com.ra.service.bean.params.OrderQueryResp;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.PayOrderTotalVo;
import com.ra.service.bean.resp.PayOrderVo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PayOrderService extends BaseService<PayOrderRepository> {

    /**
     * 商户查询订单
     * @return
     */
    List<OrderQueryResp> findMerchantQueryOtcOrder(OrderQueryParams params);


    /**
     * 查询上一次由于网络或者某些原因创建的重复订单
     * @return
     */
    PayOrder findOldCreateOrder(String outOrderNo,long merchantUserId);
    /**
     * 商户创建订单
     * @param params
     * @return
     */
    CreateOrderResp createOrder(OrderCreateParams params);

    /**
     * 订单交易订单
     * @param payOrder
     * @return
     */
    boolean successOrder(PayOrder payOrder,String statusDesc);

    /**
     * 定时器-确认收款后异步通知商户站点
     */
    void notifyTaskOrder();

    /**
     * 定时器批量关闭失败的订单
     */
    void closeTaskOrder();
    /**
     * 订单查询列表
     * @param payOrderReq
     * @param pager
     * @return
     */
    Page<PayOrderVo> findListByPayOrder(PayOrderReq payOrderReq, Pager pager);

    /**
     * 订单统计
     * @return
     */
    PayOrderTotalVo findByPayOrderTotal(PayOrderReq payOrderReq);

    /**
     * 代理的商户订单查询列表
     * @param payOrderReq
     * @param pager
     * @return
     */
    Page<PayOrderVo> findListByProxyPayOrder(PayOrderReq payOrderReq,long proxyId, Pager pager);


    /**
     * 商户查看自己订单列表
     * @param payOrderReq
     * @param pager
     * @return
     */
    Page<PayOrderVo> findListByMerchantPayOrder(PayOrderReq payOrderReq,long merchangUserId, Pager pager);
    /**
     * 根据代理id查看下面商户所跑金额统计
     * @param payOrderReq
     * @param userId
     * @return
     */
    BigDecimal findByProxyPayOrderTotal(PayOrderReq payOrderReq, long userId);

    /**
     * 商户总成交订单金额
     * @param payOrderReq
     * @param userId
     * @return
     */
    BigDecimal findByMerchantPayOrderTotal(PayOrderReq payOrderReq, long userId);
}

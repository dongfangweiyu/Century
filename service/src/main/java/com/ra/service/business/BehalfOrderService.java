package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.BehalfOrder;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.business.BehalfOrderRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.params.*;
import com.ra.service.bean.req.BehalfOrderReq;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.*;
import org.springframework.data.domain.Page;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

public interface BehalfOrderService extends BaseService<BehalfOrderRepository> {

    /**
     * 商户查询订单
     * @return
     */
    List<BehalfOrderQueryResp> findMerchantQueryOtcOrder(OrderQueryParams params);

    /**
     * 卡商查询属于自己的待处理订单
     * @return
     */
    List<BehalfOrderListQueryResp> findBehalfQueryOtcOrder(OrderQueryParams params);

    /**
     * 商户查询余额
     * @param params
     * @return
     */
    Wallet findMerchantWallet(OrderQueryParams params);

    /**
     * 商户创建订单
     * @param params
     * @return
     */
    BehalfOrder createOrder(HttpServletRequest request, BehalfOrderCreateParams params);

    boolean behalfAutomaticConfirmOrder(HttpServletRequest request,BehalfOrderAutomaticParams params);

    /**
     * 查询上一次由于网络或者某些原因创建的重复订单
     * @return
     */
    BehalfOrder findOldCreateOrder(String outOrderNo, long merchantUserId);


    /**
     * 定时器-确认收款后异步通知商户站点
     */
    void notifyTaskOrder();

    /**
     *根据商户或者卡商的userID查看自己的提现订单(如果传userId为Null则查询所有提现订单列表)
     * @return
     */
    Page<BehalfOrderVo> findBehalfOrderList(BehalfOrderReq behalfOrderReq, Long merchantUserId,Long behalfUserId, Pager pager);

    /**
     * 总后台完结该代付提现订单
     * @param behalfOrder 需要处理的订单信息
     * @param user 处理人信息
     * @return
     */
    boolean compeleteBehalfOrder(BehalfOrder behalfOrder, User user);

    /**
     * 取消订单
     * @param behalfOrder
     * @return
     */
    boolean cancelBehalfOrder(BehalfOrder behalfOrder,String statusDesc);

    /**
     * 确认付款订单
     * @param orderId
     * @return
     */
    boolean confirmOrder(long orderId, String paymentVoucher, String remark);

    /**
     * 总代付总金额
     * @param behalfOrderReq
     * @param merchantUserId 商户userId
     *  @param behalfUserId 卡商userId
     * @return
     */
    BehalfOrderTotalVo findByBehalfOrderTotal(BehalfOrderReq behalfOrderReq, Long merchantUserId,Long behalfUserId);


}

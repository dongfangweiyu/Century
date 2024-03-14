package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.Enum.OrderStatusEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.WithdrawlOrder;
import com.ra.dao.entity.security.User;
import com.ra.dao.factory.ConfigFactory;
import com.ra.dao.repository.business.WithdrawOrderRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.WithdrawOrderReq;
import com.ra.service.bean.resp.MerchantInfoVo;
import com.ra.service.bean.resp.WithdrawOrderVo;
import com.ra.service.business.WalletService;
import com.ra.service.business.WithdrawOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WithdrawOrderServiceImpl extends BaseServiceImpl<WithdrawOrderRepository> implements WithdrawOrderService {
    @Autowired
    WalletService walletService;

    @Override
    public Page<WithdrawOrderVo> findWithdrawOrderList(WithdrawOrderReq withdrawOrderReq,Long userId, Pager pager) {
        StringBuilder builder = new StringBuilder("select w.*,u.account as withdrawAccount from business_order_withdraw w LEFT JOIN security_user u on w.merchantUserId=u.id ");
        builder.append("  where 1=1 ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_order_withdraw w  ");
        sqlCount.append(" where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(userId)) {
            builder.append("  and w.merchantUserId= :userId");
            sqlCount.append("  and w.merchantUserId= :userId ");
            params.put("userId",userId);
        }
        if (!StringUtils.isEmpty(withdrawOrderReq.getStatus())) {
            builder.append("  and w.status = :status");
            sqlCount.append("  and w.status = :status");
            params.put("status",withdrawOrderReq.getStatus());
        }
        if (!StringUtils.isEmpty(withdrawOrderReq.getBeginTime())) {
            builder.append(" and w.createTime >= :beginTime");
            sqlCount.append(" and w.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(withdrawOrderReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(withdrawOrderReq.getEndTime())) {
            builder.append(" and w.createTime <= :endTime");
            sqlCount.append(" and w.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(withdrawOrderReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by w.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<WithdrawOrderVo> list = findList(builder.toString(), params, WithdrawOrderVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    @Transactional
    public boolean addWithdrawal(WithdrawlOrder withdrawlOrder) {
        WithdrawlOrder withdrawlOrder1= repository.save(withdrawlOrder);
        if(withdrawlOrder1!=null){
            int i=walletService.subMoney(withdrawlOrder1.getMerchantUserId(),withdrawlOrder1.getAmount(), WalletLogEnum.MERCHANTWITHDRAWAL);
            if(i>0){
                return true;
            }
            throw new IllegalArgumentException("发起提现失败！");
        }
        return false;
    }

    @Override
    @Transactional
    public boolean meDealOrder(long orderId, String dealAccount) {
        int i = getRepository().meDealWithdarwOrder(orderId,dealAccount);
        if(i>0){
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean confirmOrder(long orderId, String paymentVoucher, String remark) {
        WithdrawlOrder order = getRepository().findWithdrawlOrderById(orderId);
        Assert.notNull(order,"订单不存在");
        if(order.getStatus()!= OrderStatusEnum.PROCESS){
            throw new IllegalArgumentException("只有交易中状态才能确认付款");
        }
        int i = getRepository().confirmOrder(orderId,paymentVoucher,remark);
        if(i>0){
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean compeleteOrder(long orderId,long userId) {
        WithdrawlOrder order = getRepository().findWithdrawlOrderById(orderId);
        Assert.notNull(order,"订单不存在");
        if(order.getMerchantUserId().longValue()!=userId){
            throw new IllegalArgumentException("没有权限");
        }
        int i =getRepository().successOrder(orderId);
        if(i>0){
            BigDecimal amount = ConfigFactory.getBigDecimal(ConfigEnum.WITHDRAW_RATE);
            i = walletService.subMoney(order.getMerchantUserId(), amount, WalletLogEnum.WITHDRAWALRATE);//扣除提现手续费
            if(i>0){
                return true;
            }
        }
        throw new IllegalArgumentException("操作失败");
    }


    /**
     * 取消提现
     * @param withdrawOrderId
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public boolean cancelWithdrawOrder(long withdrawOrderId, long userId,String remark) {
        WithdrawlOrder withdrawlOrder = getRepository().findWithdrawlOrderById(withdrawOrderId);
        Assert.notNull(withdrawlOrder,"该提现订单不存在");
        if(withdrawlOrder.getMerchantUserId().longValue()!=userId){
            throw new IllegalArgumentException("没有权限");
        }
        int i = getRepository().cannelWithdarwOrder(withdrawOrderId,remark);//将该订单状态改成失败，即取消
        if(i>0){//状态取消成功后，把取消的金额返回到账户上
            walletService.addMoney(userId,withdrawlOrder.getAmount(),WalletLogEnum.WITHDRAWALCANCEL);
            return true;
        }
        return false;
    }
}

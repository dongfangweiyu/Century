package com.ra.service.security;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.entity.business.PayChannel;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.security.UserRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.AgencyQueryReq;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.resp.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author huli
 * @time 2019-01-02
 * @description
 */
public interface UserService extends BaseService<UserRepository> {


    /**
     * 后台登录
     * @param account
     * @param password
     * @return
     */
    User loginAdmin(HttpServletRequest request,String account, String password);


    /**
     * 查询代理信息列表
     */
    Page<ProxyInfoVo> findListProxyInfo(AgencyQueryReq agencyQueryReq, Pager pager);

    MerchantInfoTotalVo findProxyInfoTotal(AgencyQueryReq agencyQueryReq);
    /**
     * 查询所有代理信息下拉框
     */
    List<User> findAllProxyList();
    /**
     * 添加代理商
     * @param
     * @return
     */
    boolean addAgencyUser(ProxyAddInfoVo proxyAddInfoVo);


    /**
     * 添加商户
     * @param addMechantInfoVo
     * @return
     */
    boolean addMerchantUser(AddMechantInfoVo addMechantInfoVo, ApplyMerchantEnum applyMerchantEnum);

    /**
     * 编辑商户
     * @param addMechantInfoVo
     * @return
     */
    boolean editMerchantUser(AddMechantInfoVo addMechantInfoVo);

    /**
     * 根据机构，获取user列表
     * @param agencyEnum
     * @return
     */
    List<UserAdminVo> findUserByAgency(AgencyEnum agencyEnum);


    /**
     * 查询该代理下商户账号列表下拉框
     * @param proxyUserId
     * @return
     */
    List<User> findProxyMerchantAccountList(long proxyUserId);
    /**
     * 添加后台管理员
     * @return
     */
    boolean addAdminUser(User user);

    /**
     * 重置密码为888888
     * @param userId
     * @return
     */
    boolean resetPassword(long userId);

    /**
     * 重置支付密码为888888
     * @param userId
     * @return
     */
    boolean resetPayPassword(long userId);

    /**
     * 添加卡商
     * @param addBehalfInfoVo
     * @return
     */
    boolean addBehalfUser(AddBehalfInfoVo addBehalfInfoVo);

    /**
     * 编辑卡商
     */
    boolean editBehalfUser(AddBehalfInfoVo addBehalfInfoVo);
}
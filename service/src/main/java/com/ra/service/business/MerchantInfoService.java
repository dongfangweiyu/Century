package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.repository.business.MerchantInfoRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.MerchantQueryReq;
import com.ra.service.bean.req.OpenAccountReq;
import com.ra.service.bean.resp.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author jj
 * @time 2019-01-24
 * @description
 */
public interface MerchantInfoService extends BaseService<MerchantInfoRepository> {

    /**
     * 查询商户信息
     * @param appId
     * @return
     */
    MerchantUserInfoVo findMerchantInfo(String appId);


    /**
     * 查询商户信息列表
     */
    Page<MerchantInfoVo> findListMerchantInfo(MerchantQueryReq merchantQueryReq, Pager pager);


    MerchantInfoTotalVo findMerchantTotal(MerchantQueryReq merchantQueryReq);

    /**
     * 查询代理商户信息列表
     */
    Page<MerchantInfoVo> findListProxyMerchantInfo(MerchantQueryReq agencyQueryReq,long proxyUserId, Pager pager);

    MerchantInfo findByUserId(Long userId);


    /**
     * 商户申请
     * @param applyStatua 状态
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param companyName 公司名称
     * @param pager
     * @return
     */
    Page<MerchantInfo> findListByApplyMerchant(Long proxyId,String applyStatua, String beginTime, String endTime, String companyName, Pager pager);


    /**
     * 开户管理
     * @param pager
     * @return
     */
    Page<OpenAccountVo> findOpenAccount(OpenAccountReq openAccountReq, Pager pager);

    boolean applyOpenPass(Long id);

    /**
     * 批量下代付订单
     * @param batchBehalfOrderDataVo 批量下单的数据data
     * @param merchantUserId 下单商户
     * @return
     */
    boolean batchBehalfOrder(BatchBehalfOrderDataVo batchBehalfOrderDataVo,long merchantUserId,String notifyUrl);

    /**
     * 查询与卡商绑定卡组的商户余额信息
     */
    Page<MerchantInfoVo> findListBehalfMerchantInfo(MerchantQueryReq agencyQueryReq,long behalfUserId, Pager pager);

    MerchantInfoTotalVo findBehalfMerchantTotal(MerchantQueryReq merchantQueryReq,long behalfUserId);

    /**
     * 批量文本导入
     * @param batchBehalfDataVoList 文本路径
     * @param merchantUserId 商户Id
     * @param notifyUrl 回调地址
     * @return
     */
    boolean bulkTextBehalfOrder(List<BatchBehalfDataVo> batchBehalfDataVoList,long merchantUserId,String notifyUrl);

}

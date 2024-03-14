package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.BehalfInfo;
import com.ra.dao.repository.business.BehalfInfoRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.BehalfQueryReq;
import com.ra.service.bean.resp.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author jj
 * @time 2020-10-12
 * @description
 */
public interface BehalfInfoService extends BaseService<BehalfInfoRepository> {

    BehalfInfo findByUserId(Long userId);
    /**
     * 查询卡商信息
     * @param appId
     * @return
     */
    BehalfUserInfoVo findBehalfInfo(String appId);
    /**
     * 查询卡商信息列表
     */
    Page<BehalfInfoVo> findListBehalfInfo(BehalfQueryReq behalfQueryReq, Pager pager);


    /**
     * 查询所有卡商剩余代发额度
     * @param behalfQueryReq
     * @return
     */
    BehalfInfoTotalVo findBehalfTotal(BehalfQueryReq behalfQueryReq);

    /**
     * 查询所有可用的卡商信息
     * @return
     */
    List<BehalfInfo> findEnbleBehalfInfoList();

    /**
     * 扣除利润
     * @param profit
     * @param userId
     * @return
     */
    boolean subProfit(BigDecimal profit, Long userId, WalletLogEnum walletLogEnum);

    /**
     * 增加利润
     * @param profit
     * @param userId
     * @return
     */
    boolean addProfit(BigDecimal profit, Long userId,WalletLogEnum walletLogEnum);


}

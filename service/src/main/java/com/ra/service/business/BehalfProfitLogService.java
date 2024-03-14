package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.repository.business.BehalfProfitLogRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.BehalfProfitLogVo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface BehalfProfitLogService extends BaseService<BehalfProfitLogRepository> {
    /**
     * 根据卡商userId获取信息
     * 保存卡商利润变动日记
     * @return
     */
    boolean saveBehalfProfitLog(long behalfUserId, BigDecimal amount, String remark);

    /**
     * 卡商的利润变动日志
     */
    Page<BehalfProfitLogVo> findBehalfProfitLogList(WalletLogReq walletLogReq, Pager pager);

    /**
     * 利润总金额
     * @param walletLogReq
     * @return
     */
    BigDecimal findByBehalfProfitTotal(WalletLogReq walletLogReq);

}

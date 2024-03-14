package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.business.WalletLog;
import com.ra.dao.repository.business.WalletLogRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.WalletLogTimeVo;
import com.ra.service.bean.resp.WalletLogVo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * @author huli
 * @time 2019-01-24
 * @description
 */
public interface WalletLogService extends BaseService<WalletLogRepository> {

    /**
     * 根据userId查询佣金提现记录
     * @param userId
     * @param pager
     * @return
     */
    Page<WalletLog> findWalletLogWithdraw(long userId, Pager pager);

    /**
     * 根据userId获取钱包
     * 保存金额变动日记
     * @return
     */
    boolean saveWalletLog(long userId, BigDecimal amount, WalletLogEnum logEnum);

    /**
     * 传入钱包对象
     * 保存金额变动日记
     * @return
     */
    boolean saveWalletLog(Wallet wallet, BigDecimal amount, WalletLogEnum logEnum);

    /**
     *代理或商户根据id看自己下面的金额变动日志
     */
    Page<WalletLogVo> findWalletLogList(WalletLogReq walletLogReq, long userId, Pager pager);

    /**
     * 总后台金额变动日志
     * @param walletLogReq
     * @param pager
     * @return
     */
    Page<WalletLogVo> findAdminWalletLogList(WalletLogReq walletLogReq, Pager pager);

    /**
     * 总后台瞬时查用户余额
     * @param walletLogReq
     * @param pager
     * @return
     */
    Page<WalletLogTimeVo> findWalletLogTimeList(WalletLogReq walletLogReq, Pager pager);

    /**
     * 总后台瞬时查用户余额，总额统计
     * @param walletLogReq
     * @return
     */
    BigDecimal findWalletLogTimeTotal(WalletLogReq walletLogReq);
    /**
     * 总额
     * @param walletLogReq
     * @param userId
     * @return
     */
    double findByWalletLogTotal(WalletLogReq walletLogReq, long userId);
}

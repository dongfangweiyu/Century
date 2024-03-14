package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.ChannelWalletLog;
import com.ra.dao.entity.business.Wallet;
import com.ra.dao.entity.business.WalletLog;
import com.ra.dao.repository.business.ChannelWalletLogRepository;
import com.ra.dao.repository.business.WalletLogRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.ChannelWalletLogReq;
import com.ra.service.bean.req.WalletLogReq;
import com.ra.service.bean.resp.ChannelWalletLogTimeVo;
import com.ra.service.bean.resp.ChannelWalletLogVo;
import com.ra.service.bean.resp.WalletLogTimeVo;
import com.ra.service.bean.resp.WalletLogVo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * @author huli
 * @time 2019-01-24
 * @description
 */
public interface ChannelWalletLogService extends BaseService<ChannelWalletLogRepository> {

    /**
     * 根据userId查询佣金提现记录
     * @param pager
     * @return
     */
    Page<ChannelWalletLogVo> findChannelWalletLogList(ChannelWalletLogReq channelWalletLogReq, Pager pager);

    /**
     * 根据配置ID获取钱包
     * 保存通道金额变动日记
     * @return
     */
    boolean saveWalletLog(long configPayInterfaceId, BigDecimal amount, String description);

    /**
     * 总后台瞬时查通道余额
     * @param channelWalletLogReq
     * @param pager
     * @return
     */
    Page<ChannelWalletLogTimeVo> findChannelWalletLogTimeList(ChannelWalletLogReq channelWalletLogReq, Pager pager);

    /**
     * 总后台瞬时查通道余额，总额统计
     * @param channelWalletLogReq
     * @return
     */
    BigDecimal findChannelWalletLogTimeTotal(ChannelWalletLogReq channelWalletLogReq);


}

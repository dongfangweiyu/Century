package com.ra.service.business;

import com.ra.common.domain.Pager;
import com.ra.dao.entity.business.BehalfOrder;
import com.ra.dao.entity.business.PayOrder;
import com.ra.dao.repository.business.IncomeLogRepository;
import com.ra.service.base.BaseService;
import com.ra.service.bean.req.PayOrderReq;
import com.ra.service.bean.req.ProxyIncomeLogReq;
import com.ra.service.bean.resp.IncomeTotalVo;
import com.ra.service.bean.resp.PayOrderTotalVo;
import com.ra.service.bean.resp.ProxyIncomeLogVo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface IncomeLogService extends BaseService<IncomeLogRepository> {


    boolean saveIncomeLog(PayOrder order);

    /**
     * 平台收入列表
     */
    Page<ProxyIncomeLogVo>findListIncomeLog(ProxyIncomeLogReq proxyIncomeLogReq, Pager pager);

    /**
     * 平台收入统计
     * @param proxyIncomeLogReq
     * @return
     */
    BigDecimal findByIncomeLogTotal(ProxyIncomeLogReq proxyIncomeLogReq);

    /**
     * 保存代付提现订单完成各自的收入
     * @param behalfOrder
     * @return
     */
    boolean saveBehalfIncomeLog(BehalfOrder behalfOrder);

    /**
     * 代收和代付各自的总收入
     * @return
     */
    IncomeTotalVo findByIncomeTypeTotal(ProxyIncomeLogReq proxyIncomeLogReq);
}

package com.ra.service.business.impl;

import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.Enum.WalletLogEnum;
import com.ra.dao.entity.business.BehalfInfo;
import com.ra.dao.entity.business.MerchantInfo;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.business.BehalfInfoRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.BehalfQueryReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.BehalfInfoService;
import com.ra.service.business.BehalfProfitLogService;
import com.ra.service.business.WalletService;
import com.ra.service.security.UserService;
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

/**
 * @author ouyang
 * @time 2019-01-24
 * @description
 */
@Service
public class BehalfInfoServiceImpl extends BaseServiceImpl<BehalfInfoRepository> implements BehalfInfoService {

    @Autowired
    private WalletService walletService;
    @Autowired
    private BehalfProfitLogService behalfProfitLogService;
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public BehalfInfo findByUserId(Long userId) {
        BehalfInfo behalfInfo = getRepository().findByUserId(userId);
        Assert.notNull(behalfInfo,"卡商信息不存在");

        boolean fixInfi=false;
        if(StringUtils.isEmpty(behalfInfo.getAppId())){
            behalfInfo.setAppId(behalfInfo.generationAppId());
            fixInfi=true;
        }
        if(StringUtils.isEmpty(behalfInfo.getSecret())){
            behalfInfo.setSecret(behalfInfo.generationSecret());
            fixInfi=true;
        }
        if(fixInfi){
            behalfInfo= getRepository().save(behalfInfo);
        }
        return behalfInfo;
    }

    @Override
    public BehalfUserInfoVo findBehalfInfo(String appId) {
        BehalfInfo behalfInfo = getRepository().findByAppId(appId);
        Assert.notNull(behalfInfo,"无效的APPID");
        User user = userService.getRepository().findUserById(behalfInfo.getUserId());
        Assert.isTrue(user.getStatus()==0,"卡商权限不足,请联系客服");

        return new BehalfUserInfoVo(behalfInfo,user);
    }

    @Override
    public Page<BehalfInfoVo> findListBehalfInfo(BehalfQueryReq behalfQueryReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select b.userId as userId,u.account as behalfAccount,b.companyName as companyName,w.money as walletMoney,\n" +
                "                u.createTime as createTime,u.`status` as status,\n" +
                "        u.lastLoginTime,u.loginIp,b.behalfRate,b.behalfFee,b.profit from business_behalf_info b \n" +
                "                LEFT JOIN security_user u on b.userId=u.id  LEFT JOIN business_wallet w on b.userId=w.userId ");
        builder.append("  where 1=1  and u.agencyType='BEHALF' ");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from business_behalf_info b \n" +
                "LEFT JOIN security_user u on b.userId=u.id \n" +
                "LEFT JOIN business_wallet w on b.userId=w.userId ");
        sqlCount.append(" where 1=1  and u.agencyType='BEHALF' ");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(behalfQueryReq.getQueryParam())) {
            builder.append("  and u.account like :account");
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+behalfQueryReq.getQueryParam()+'%');
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getStatus())) {
            builder.append("  and u.status = :status");
            sqlCount.append("  and u.status = :status");
            params.put("status",behalfQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getBeginTime())) {
            builder.append(" and u.createTime >= :beginTime");
            sqlCount.append(" and u.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(behalfQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getEndTime())) {
            builder.append(" and u.createTime <= :endTime");
            sqlCount.append(" and u.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(behalfQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }

        int count = findCount(sqlCount.toString(), params);
        builder.append(" order by u.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<BehalfInfoVo> list = findList(builder.toString(), params, BehalfInfoVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public BehalfInfoTotalVo findBehalfTotal(BehalfQueryReq behalfQueryReq) {
        StringBuffer sqlCount=new StringBuffer(" select ifnull(sum(w.money),0) as totalBalance from business_behalf_info b \n" +
                "LEFT JOIN security_user u on b.userId=u.id \n" +
                "LEFT JOIN business_wallet w on b.userId=w.userId ");
        sqlCount.append(" where 1=1  and u.agencyType='BEHALF' ");
        Map<String, Object> params = new HashMap<>();
        if (!StringUtils.isEmpty(behalfQueryReq.getQueryParam())) {
            sqlCount.append("  and u.account like :account");
            params.put("account", '%'+behalfQueryReq.getQueryParam()+'%');
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getStatus())) {
            sqlCount.append("  and u.status = :status");
            params.put("status",behalfQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getBeginTime())) {
            sqlCount.append(" and u.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(behalfQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(behalfQueryReq.getEndTime())) {
            sqlCount.append(" and u.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(behalfQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        return findOne(sqlCount.toString(),params,BehalfInfoTotalVo.class);
    }

    @Override
    public List<BehalfInfo> findEnbleBehalfInfoList() {
        Map<String,Object> params=new HashMap<>();
        String sql="SELECT b.* FROM business_behalf_info b left JOIN security_user u on b.userId=u.id where u.agencyType='BEHALF' and u.`status`=0";
        return findList(sql, params, BehalfInfo.class);
    }

    @Override
    @Transactional
    public boolean subProfit(BigDecimal profit, Long userId,WalletLogEnum walletLogEnum) {
        boolean b=behalfProfitLogService.saveBehalfProfitLog(userId,BigDecimal.ZERO.subtract(profit),walletLogEnum.getDescription());
        if(b){
            int i = getRepository().subMoney(profit, userId);
            if(i>0){
                i=walletService.subMoney(userId,profit,walletLogEnum);
                if(i>0){
                    return true;
                }
            }
        }
        throw new IllegalArgumentException("扣除利润失败");
    }

    @Override
    @Transactional
    public boolean addProfit(BigDecimal profit, Long userId, WalletLogEnum walletLogEnum) {
        boolean b=behalfProfitLogService.saveBehalfProfitLog(userId,profit,walletLogEnum.getText());
        if(b){
            int i = getRepository().addMoney(profit, userId);
            if(i>0){
                return true;
            }
        }
        throw new IllegalArgumentException("增加利润失败");
    }


}

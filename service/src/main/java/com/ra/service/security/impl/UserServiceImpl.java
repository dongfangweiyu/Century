package com.ra.service.security.impl;

import com.ra.common.component.RedisComponent;
import com.ra.common.constant.RedisConstant;
import com.ra.common.domain.Pager;
import com.ra.common.domain.PagerImpl;
import com.ra.common.utils.DateUtil;
import com.ra.common.utils.EncryptUtil;
import com.ra.common.utils.IpUtil;
import com.ra.dao.Enum.AgencyEnum;
import com.ra.dao.Enum.ApplyMerchantEnum;
import com.ra.dao.entity.business.*;
import com.ra.dao.entity.security.User;
import com.ra.dao.repository.security.UserRepository;
import com.ra.service.base.impl.BaseServiceImpl;
import com.ra.service.bean.req.AgencyQueryReq;
import com.ra.service.bean.resp.*;
import com.ra.service.business.BehalfInfoService;
import com.ra.service.business.MerchantInfoService;
import com.ra.service.business.RateService;
import com.ra.service.business.WalletService;
import com.ra.service.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author huli
 * @time 2019-01-02
 * @description
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<UserRepository> implements UserService {

    private static  final Logger logger= LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private MerchantInfoService merchantInfoService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private RateService rateService;
    @Autowired
    private BehalfInfoService behalfInfoService;

    @Override
    @Transactional
    public User loginAdmin(HttpServletRequest request,String account, String password) {
        long passwordCount = redisComponent.incr(RedisConstant.PASSWORDERRORLIMIT + account,0);//密码错误次数
        if(passwordCount>=5){
            long expire = redisComponent.getExpire(RedisConstant.PASSWORDERRORLIMIT + account);
            throw new IllegalArgumentException("密码输入错误次数太多请"+expire+"秒后再试");
        }
        User user = repository.findUserByAccountAndPassword(account, password);
        if(user==null){
            redisComponent.incr(RedisConstant.PASSWORDERRORLIMIT + account, 1);//记录密码错误次数
            redisComponent.expire(RedisConstant.PASSWORDERRORLIMIT+account,30*60);//输错3次，禁止登陆30分钟
            throw new IllegalArgumentException("账户或密码错误");
        }

        Assert.isTrue(!user.isDeleted(), "用户不存在");
        Assert.isTrue(user.getStatus() ==0, "账号已被禁用");

        user.setLoginIp(IpUtil.getIpAddr(request));
        user.setLastLoginTime(new Timestamp(System.currentTimeMillis()));
        user=getRepository().save(user);
        return user;
    }

    @Override
    public Page<ProxyInfoVo> findListProxyInfo(AgencyQueryReq agencyQueryReq, Pager pager) {
        StringBuilder builder = new StringBuilder("select p.id as userId,p.account,p.createTime,p.status,p.lastLoginTime,p.loginIp,w.money as walletMoney from security_user p left join business_wallet w on p.id=w.userId ");
        builder.append(" where 1=1  and p.agencyType='PROXY'");
        StringBuffer sqlCount=new StringBuffer(" select count(1) from security_user p");
        sqlCount.append(" where 1=1  and p.agencyType='PROXY'");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(agencyQueryReq.getQueryParam())) {
            builder.append("  and p.account like :account");
            sqlCount.append("  and p.account like :account");
            params.put("account", '%'+agencyQueryReq.getQueryParam()+'%');
        }

        if (!StringUtils.isEmpty(agencyQueryReq.getStatus())) {
            builder.append("  and p.status = :status");
            sqlCount.append("  and p.status = :status");
            params.put("status",agencyQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(agencyQueryReq.getBeginTime())) {
            builder.append(" and p.createTime >= :beginTime");
            sqlCount.append(" and p.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(agencyQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(agencyQueryReq.getEndTime())) {
            builder.append(" and p.createTime <= :endTime");
            sqlCount.append(" and p.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(agencyQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }


        int count = findCount(sqlCount.toString(), params);
//        int count = findCount(sqlCount.toString(), new HashMap<>());
        builder.append(" order by p.createTime desc limit :pageNo,:pageSize");
        params.put("pageNo",(pager.getPage()-1)*pager.getSize());
        params.put("pageSize",pager.getSize());
        List<ProxyInfoVo> list = findList(builder.toString(), params, ProxyInfoVo.class);
        return new PagerImpl(list,pager.of(),count);
    }

    @Override
    public MerchantInfoTotalVo findProxyInfoTotal(AgencyQueryReq agencyQueryReq) {
        StringBuffer sqlCount=new StringBuffer(" select ifnull(sum(w.money),0) as totalBalance from security_user p left join business_wallet w on p.id=w.userId ");
        sqlCount.append(" where 1=1  and p.agencyType='PROXY'");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isEmpty(agencyQueryReq.getQueryParam())) {
            sqlCount.append("  and p.account like :account");
            params.put("account", '%'+agencyQueryReq.getQueryParam()+'%');
        }

        if (!StringUtils.isEmpty(agencyQueryReq.getStatus())) {
            sqlCount.append("  and p.status = :status");
            params.put("status",agencyQueryReq.getStatus());
        }
        if (!StringUtils.isEmpty(agencyQueryReq.getBeginTime())) {
            sqlCount.append(" and p.createTime >= :beginTime");
            params.put("beginTime", DateUtil.stringToDate(agencyQueryReq.getBeginTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        if (!StringUtils.isEmpty(agencyQueryReq.getEndTime())) {
            sqlCount.append(" and p.createTime <= :endTime");
            params.put("endTime",DateUtil.stringToDate(agencyQueryReq.getEndTime(),"yyyy-MM-dd HH:mm:ss"));
        }
        return findOne(sqlCount.toString(),params,MerchantInfoTotalVo.class);
    }

    @Override
    public List<User> findAllProxyList() {
        Map<String,Object> params=new HashMap<>();
        String sql="select a.* from security_user as a where a.agencyType='PROXY'";
        return findList(sql, params, User.class);
    }


    @Override
    @Transactional
    public boolean addAgencyUser(ProxyAddInfoVo proxyAddInfoVo) {
        proxyAddInfoVo.setAccount(proxyAddInfoVo.getAccount()+"@proxy.com");
        User old = repository.findUserByAccount(proxyAddInfoVo.getAccount());
        Assert.isNull(old, "用户已存在");
        User user=new User();
        user.setAccount(proxyAddInfoVo.getAccount());
        String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("888888"));
        user.setPassword(passwordAsMd5);
        user.setStatus(0);
        user.setAgencyType(AgencyEnum.PROXY);
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user = repository.save(user);

        //创建钱包
        Wallet wallet=new Wallet();
        wallet.setMoney(BigDecimal.ZERO);
        wallet.setUserId(user.getId());
        walletService.getRepository().save(wallet);

        return true;
    }

    @Override
    @Transactional
    public boolean addMerchantUser(AddMechantInfoVo addMechantInfoVo, ApplyMerchantEnum applyMerchantEnum) {
        addMechantInfoVo.setMerchantAccount(addMechantInfoVo.getMerchantAccount()+"@merchant.com");
        User old = repository.findUserByAccount(addMechantInfoVo.getMerchantAccount());
        Assert.isNull(old, "用户已存在");
        User user=new User();
        user.setAccount(addMechantInfoVo.getMerchantAccount());
        String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("888888"));
        user.setPassword(passwordAsMd5);

        if(applyMerchantEnum== ApplyMerchantEnum.PASS){
            user.setStatus(0);
        }else{
            user.setStatus(1);
        }
        user.setAgencyType(AgencyEnum.MERCHANT);
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user = repository.save(user);

        //创建钱包
        Wallet wallet=new Wallet();
        wallet.setMoney(BigDecimal.ZERO);
        wallet.setUserId(user.getId());
        walletService.getRepository().save(wallet);


        //创建商户
        MerchantInfo merchantInfo=new MerchantInfo();
        merchantInfo.setUserId(user.getId());
        merchantInfo.setCompanyName(addMechantInfoVo.getCompanyName());
        if(addMechantInfoVo.getProxyUserId()!=null&&addMechantInfoVo.getProxyUserId()>0){
            merchantInfo.setProxyUserId(addMechantInfoVo.getProxyUserId());
        }
        merchantInfo.setAppId(merchantInfo.generationAppId());
        merchantInfo.setSecret(merchantInfo.generationSecret());
        merchantInfo.setApplyStatus(applyMerchantEnum);
        merchantInfo=merchantInfoService.getRepository().save(merchantInfo);

        List<Rate> rateList=new ArrayList<>();
        for(RateVo rateVo:addMechantInfoVo.getRateVoList()){
            Rate rate=new Rate();
            if(rateVo.isChecked()){
                //如果代理的费率大于商户的费率
                if(rateVo.getProxyRate().compareTo(rateVo.getMerchantRate())==1){
                    throw new IllegalArgumentException("代理的费率不能高于商户费率");
                }
                rate.setMerchantInfoId(merchantInfo.getId());
                rate.setPayChannelId(rateVo.getPayChannelId());
                rate.setMerchantRate(rateVo.getMerchantRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setProxyRate(rateVo.getProxyRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setWeight(rateVo.getWeight());
                rateList.add(rate);
            }
        }
        rateService.getRepository().saveAll(rateList);
        return true;
    }

    @Override
    @Transactional
    public boolean editMerchantUser(AddMechantInfoVo addMechantInfoVo) {

        MerchantInfo merchantInfo=merchantInfoService.getRepository().findByUserId(addMechantInfoVo.getUserId());//查询商户信息
        merchantInfo.setCompanyName(addMechantInfoVo.getCompanyName());
        merchantInfoService.getRepository().save(merchantInfo);

//        rateService.getRepository().deleteByMerchantInfoId(merchantInfo.getId());

        List<Rate> rateList=new ArrayList<>();
        List<Rate> deleteList=new ArrayList<>();
        for(RateVo rateVo:addMechantInfoVo.getRateVoList()){
            Rate rate=new Rate();
            if(rateVo.isChecked()){
                //如果代理的费率大于商户的费率
                if(rateVo.getProxyRate().compareTo(rateVo.getMerchantRate())==1){
                    throw new IllegalArgumentException("代理的费率不能高于商户费率");
                }

                rate.setId(rateVo.getId());
                rate.setMerchantInfoId(merchantInfo.getId());
                rate.setPayChannelId(rateVo.getPayChannelId());
                rate.setMerchantRate(rateVo.getMerchantRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setProxyRate(rateVo.getProxyRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
                rate.setWeight(rateVo.getWeight());
                rateList.add(rate);
            }else if(rateVo.getId()!=null&&rateVo.getId()>0){
                rate.setId(rateVo.getId());
                deleteList.add(rate);
            }
        }
        rateService.getRepository().saveAll(rateList);
        rateService.getRepository().deleteAll(deleteList);
        return true;
    }


    @Override
    public List<UserAdminVo> findUserByAgency(AgencyEnum agencyType) {
        Map<String,Object> params=new HashMap<>();
        params.put("agencyType",agencyType.toString());
        String sql="select a.*,b.name as roleName from security_user as a left join security_role as b on a.roleId=b.id where agencyType = :agencyType order by createTime asc";
        return findList(sql,params,UserAdminVo.class);
    }

    @Override
    public List<User> findProxyMerchantAccountList(long proxyUserId) {
        Map<String,Object> params=new HashMap<>();
        params.put("proxyUserId",proxyUserId);
        String sql="select a.* from security_user as a left join business_merchant_info as b on a.id=b.userId where b.proxyUserId=:proxyUserId";
        return findList(sql, params, User.class);
    }

    /**
     * 添加管理员
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean addAdminUser(User user) {
        String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5(user.getPassword()));
        user.setId(null);
        user.setAccount(user.getAccount()+"@admin.com");
        user.setPassword(passwordAsMd5);
        user.setStatus(0);
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user.setAgencyType(AgencyEnum.ADMIN);
        User save = getRepository().save(user);
        return save!=null;
    }

    @Override
    public boolean resetPassword(long userId) {
        User user = getRepository().findUserById(userId);
        Assert.notNull(user,"用户不存在");
        user.setPassword(EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("888888")));
        User save = getRepository().save(user);
        if(save!=null){
            redisComponent.del(RedisConstant.PASSWORDERRORLIMIT+save.getAccount());//清除错误密码次数记录
            return true;
        }
        return false;
    }

    @Override
    public boolean resetPayPassword(long userId) {
        User user = getRepository().findUserById(userId);
        Assert.notNull(user,"用户不存在");
        user.setPay_password(EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("888888")));
        User save = getRepository().save(user);
        if(save!=null){
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean addBehalfUser(AddBehalfInfoVo addBehalfInfoVo) {
        addBehalfInfoVo.setBehalfAccount(addBehalfInfoVo.getBehalfAccount()+"@behalf.com");
        User old = repository.findUserByAccount(addBehalfInfoVo.getBehalfAccount());
        Assert.isNull(old, "用户已存在");
        User user=new User();
        user.setAccount(addBehalfInfoVo.getBehalfAccount());
        String passwordAsMd5 = EncryptUtil.encodeSHA(EncryptUtil.encodeMD5("888888"));
        user.setPassword(passwordAsMd5);
        user.setStatus(0);
        user.setAgencyType(AgencyEnum.BEHALF);
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user = repository.save(user);

        //创建钱包
        Wallet wallet=new Wallet();
        wallet.setMoney(BigDecimal.ZERO);
        wallet.setUserId(user.getId());
        walletService.getRepository().save(wallet);


        //创建卡商
        BehalfInfo behalfInfo=new BehalfInfo();
        behalfInfo.setUserId(user.getId());
        behalfInfo.setCompanyName(addBehalfInfoVo.getCompanyName());
        behalfInfo.setBehalfRate(addBehalfInfoVo.getBehalfRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
        behalfInfo.setBehalfFee(addBehalfInfoVo.getBehalfFee());
        behalfInfo.setAppId(behalfInfo.generationAppId());
        behalfInfo.setSecret(behalfInfo.generationSecret());
        behalfInfo.setProfit(BigDecimal.ZERO);

        behalfInfoService.getRepository().save(behalfInfo);
        return true;
    }

    @Override
    @Transactional
    public boolean editBehalfUser(AddBehalfInfoVo addBehalfInfoVo) {
        BehalfInfo behalfInfo=behalfInfoService.getRepository().findByUserId(addBehalfInfoVo.getUserId());//查询商户信息
        behalfInfo.setBehalfFee(addBehalfInfoVo.getBehalfFee());
        behalfInfo.setCompanyName(addBehalfInfoVo.getCompanyName());
        behalfInfo.setBehalfRate(addBehalfInfoVo.getBehalfRate().divide(BigDecimal.valueOf(100)).setScale(4, BigDecimal.ROUND_UNNECESSARY));
        behalfInfoService.getRepository().save(behalfInfo);
        return  true;
    }

}
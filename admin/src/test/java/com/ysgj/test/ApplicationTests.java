package com.ysgj.test;

import com.alibaba.fastjson.JSON;
import com.ra.admin.Admin;
import com.ra.dao.entity.business.PayChannel;
import com.ra.service.bean.resp.PayChannelVo;
import com.ra.service.business.PayChannelService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Admin.class)
public class ApplicationTests {

	@Autowired
	private PayChannelService payChannelService;

	@Test
	public void contextLoads() {
		Map<String,Object> params=new HashMap<>();
		params.put("merchantInfoId",1l);
		params.put("payCodeId",3l);
		params.put("amount",BigDecimal.valueOf(500));
		String sql="select a.*,b.weight from business_payChannel as a left join business_rate as b on a.id=b.payChannelId " +
				"left join business_payCode2Channel as c on a.id=c.payChannelId "+
				"where b.merchantInfoId=:merchantInfoId and c.payCodeId=:payCodeId and (a.rate < b.merchantRate or a.rate < b.proxyRate) and a.isEnable=1 " +
				"and (a.risk=0 or (a.risk=1 and a.creditScore>=:amount and a.minAmount<=:amount and a.maxAmount>=:amount and a.beginTime <= DATE_FORMAT(now(),'%T')) and a.endTime>= DATE_FORMAT(now(),'%T'))" +
				"";
//                "ORDER BY RAND()";
//        return findOne(sql,params,PayChannel.class);
		List<PayChannelVo> list = payChannelService.findList(sql, params, PayChannelVo.class);

		int count1=0;
		int count2=0;
		int count3=0;
		for (int i=0;i<10000;i++){
			Random random = new Random();
			Integer weightSum = 0;
			PayChannel result=list.get(random.nextInt(list.size()));
			for (PayChannelVo wc : list) {
				weightSum += wc.getWeight();
			}
			if(weightSum>0){
				Integer n = random.nextInt(weightSum); // n in [0, weightSum)
				Integer m = 0;
				for (PayChannelVo wc : list) {
					if (m <= n && n < m + wc.getWeight()) {
						result=wc;
						break;
					}
					m += wc.getWeight();
				}
			}
			if(result.getChannelName().equals("GP")){
				count1++;
			}
			if(result.getChannelName().equals("神马付")){
				count2++;
			}
			if(result.getChannelName().equals("JF")){
				count3++;
			}
			System.out.println(result.getChannelName());
		}
		System.out.println("count1="+count1);
		System.out.println("count2="+count2);
		System.out.println("count3="+count3);
	}

}

package com.ra.dao.channel;

import com.ra.common.bean.LayUIData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 支付接口枚举
 */
public enum PayInterfaceEnum {

    RA("万能接口","com.ra.service.channel.RAInterface",types(new LayUIData("TYPE1","自定义类型1"),new LayUIData("TYPE2","自定义类型2"),new LayUIData("TYPE3","自定义类型3"),new LayUIData("TYPE4","自定义类型4"))),
    BEHALF("万能代付充值接口","com.ra.service.channel.BehalfInterface",types(new LayUIData("BANKCARD","代付银行卡充值"))),
    GODSPAY("GodsPay第三方平台","com.ra.service.channel.GodsPayInterface",types(new LayUIData("WXPAY","微信扫码"),new LayUIData("ALIPAY","支付宝扫码"),new LayUIData("PDD","拼多多"),new LayUIData("CNP","吹牛皮"),new LayUIData("WBHB","微博红包"))),
    QCSJ("财神接口","com.ra.service.channel.QCSJInterface",types(new LayUIData("WXPAY","微信扫码"),new LayUIData("ALIPAY","支付宝扫码"),new LayUIData("ALIPAYID","ALIPAYID"),new LayUIData("ALIPAYFIX","ALIPAYFIX"),new LayUIData("CLOUDPAY","云闪付扫码"),new LayUIData("BANKPAY","BANKPAY"))),
    YiDao("蚁道第三方平台","com.ra.service.channel.YiDaoInterface",types(new LayUIData("1","微信支付"),new LayUIData("2","支付宝"),new LayUIData("3","微信手机号转账"),new LayUIData("4","支付宝号转账"),new LayUIData("5","支付宝PID"),new LayUIData("6","拼多多微信"),new LayUIData("7","义乌购支付宝")
            ,new LayUIData("8","支付宝H5(拼多多)"),new LayUIData("9","微信H5(义乌购)"),new LayUIData("10","银行卡"))),
    YiDao2("蚁道带跳转按钮","com.ra.service.channel.YiDao2Interface",types(new LayUIData("1","微信支付"),new LayUIData("2","支付宝"),new LayUIData("3","微信手机号转账"),new LayUIData("4","支付宝号转账"),new LayUIData("5","支付宝PID"),new LayUIData("6","拼多多微信"),new LayUIData("7","义乌购支付宝")
            ,new LayUIData("8","支付宝H5(拼多多)"),new LayUIData("9","微信H5(义乌购)"),new LayUIData("10","银行卡"))),
    TXIAT("txiat话费第三方平台","com.ra.service.channel.TXIATInterface",types(new LayUIData("WXPAY","微信公众号"))),
    TL68VIP("TL68VIP扫码第三方平台","com.ra.service.channel.TL68VIPInterface",types(new LayUIData("ZFBSM","支付宝扫码")
            ,new LayUIData("WXSM","微信扫码"),new LayUIData("ZFBHB","支付宝红包"),new LayUIData("ZFBH5","支付宝H5"),new LayUIData("WXH5","微信H5")
            ,new LayUIData("WXHF","微信话费"),new LayUIData("ZFBHF","支付宝话费"),new LayUIData("PDDDF","拼多多代付"),new LayUIData("TBDF","淘宝代付"),new LayUIData("MOREN","备选方式"))),
    IEASY("Ieasy第三方平台","com.ra.service.channel.IeasyInterface",types(new LayUIData("8000","网银支付")
            ,new LayUIData("8001","快捷支付"),new LayUIData("8002","微信扫码支付"),new LayUIData("8003","微信H5支付"),new LayUIData("8004","微信公众号支付")
            ,new LayUIData("8005","微信小程序支付"),new LayUIData("8006","支付宝扫码支付"),new LayUIData("8007","支付宝H5支付"),new LayUIData("8008","支付宝服务窗支付"),new LayUIData("8009","QQ钱包扫码")
            ,new LayUIData("8010","QQ钱包H5支付"),new LayUIData("8011","京东扫码支付"),new LayUIData("8012","京东H5支付"),new LayUIData("8013","百度钱包"),new LayUIData("8014","银联二维码"))),
    JUHEZHIFUU("Juhezhifuu第三方平台","com.ra.service.channel.JuhezhifuuInterface",types(new LayUIData("alipayQrcode","支付宝扫码"),new LayUIData("alipayJump","支付宝h5"),new LayUIData("wxQrcode","微信扫码"),new LayUIData("polymeriz","聚合扫码"))),
    HGPAY("皇冠支付第三方平台","com.ra.service.channel.HGPAYInterface",types(new LayUIData("ALIPAY","支付宝"),new LayUIData("WEIXIN","微信"))),

    EOKPAY("EOK支付第三方平台","com.ra.service.channel.EOKInterface",types(new LayUIData("901","微信H5"),new LayUIData("902","支付宝H5"),new LayUIData("903","支付宝扫码"),new LayUIData("919","个码支付"))),
    KAILIPAY("凯利支付第三方平台","com.ra.service.channel.KaiLiInterface",types(new LayUIData("8000","网银支付"),new LayUIData("8001","快捷支付"),new LayUIData("8002","微信扫码支付"),new LayUIData("8003","微信H5支付")
            ,new LayUIData("8004","微信公众号支付"),new LayUIData("8005","微信小程序支付"),new LayUIData("8006","支付宝扫码支付"),new LayUIData("8007","支付宝H5支付")
            ,new LayUIData("8008","支付宝服务窗支付"),new LayUIData("8009","QQ钱包扫码"),new LayUIData("8010","QQ钱包H5支付"),new LayUIData("8011","京东扫码支付")
            ,new LayUIData("8012","京东H5支付"),new LayUIData("8013","百度钱包"),new LayUIData("8014","银联二维码"))),
    NSJPAY("新世纪支付第三方平台","com.ra.service.channel.NsjPayInterface",types(new LayUIData("ALIPAY","支付宝"),new LayUIData("WEIXIN","微信"),new LayUIData("ALIPAY_H5","支付宝H5"))),

    SMFPAY("神码付第三方平台","com.ra.service.channel.SMFInterface",types(new LayUIData("901","微信公众号"),new LayUIData("902","微信扫码支付"),new LayUIData("903","支付宝扫码支付"),new LayUIData("904","支付宝手机"),new LayUIData("905","QQ手机手机"),new LayUIData("907","网银支付"),new LayUIData("908","QQ扫码支付"),new LayUIData("909","百度钱包"),new LayUIData("910","京东支付"))),

    SJPAY("水晶支付第三方平台","com.ra.service.channel.SJPayInterface",types(new LayUIData("901","微信公众号"),new LayUIData("902","微信扫码"),new LayUIData("903","支付宝扫码"),new LayUIData("904","支付宝手机"),new LayUIData("905","QQ手机支付"),new LayUIData("907","网银支付"),new LayUIData("908","QQ扫码支付"),new LayUIData("909","百度钱包"),new LayUIData("910","京东支付"),new LayUIData("911","银联扫码"),new LayUIData("912","快捷支付"),new LayUIData("1202","璀璨扫码支付"))),

    MCPAY("MC支付第三方平台","com.ra.service.channel.MCInterface",types(new LayUIData("wx","微信"),new LayUIData("zfb","支付宝"),new LayUIData("yl","银联"))),

    PAY720("720pay支付第三方平台","com.ra.service.channel.Pay720Interface",types(new LayUIData("alipay","支付宝"),new LayUIData("wechat","微信"),new LayUIData("unionpay","云闪付"))),
    YOUYOUPAY("莜莜支付第三方平台","com.ra.service.channel.YouYouPayInterface",types(new LayUIData("963","支付宝扫码（300-2w）"),new LayUIData("964","大额网银(50-1w)"),new LayUIData("962","支付宝扫码(500-10000)"),new LayUIData("960","微信扫码（附言模式）"),new LayUIData("959","支付宝H5 100.200.话费300-8K"),new LayUIData("958","银联扫码300-5K"),new LayUIData("956","微信wap话费50-100-200"),new LayUIData("965","原生支付宝500.1K,2K,3K,4K,5K"),new LayUIData("969","QQH5(10-1W)"),new LayUIData("966","微信话费50,100,200"),new LayUIData("968","支付宝话费50,100,200"))),
    XUNHANGJIAN("巡航舰支付第三方平台","com.ra.service.channel.XunHangJianInterface",types(new LayUIData("13","支付宝"),new LayUIData("1","微信"))),
    YunPay("云支付第三方平台","com.ra.service.channel.YunPayInterface",types(new LayUIData("1","支付宝扫码"),new LayUIData("2","微信扫码"),new LayUIData("3","银行卡转账"),new LayUIData("4","微信转手机号"),new LayUIData("5","微信转银行卡"),new LayUIData("6","云闪付"),new LayUIData("11","吱口令"),new LayUIData("16","微信赞赏码"),new LayUIData("15","支付宝号转账"),new LayUIData("19","财通支付19"))),
    WANSHITONG("万事通第三方平台","com.ra.service.channel.WanShiTongInterface",types(new LayUIData("alipay","支付宝"),new LayUIData("wechat","微信"))),
   // MianQian("免签支付第三方平台","com.ra.service.channel.MianQianPayInterface",types(new LayUIData("bank2alipay","网银"))),
    UCPAY("UC支付第三方平台","com.ra.service.channel.UCPayInterface",types(new LayUIData("wechat","微信"),new LayUIData("alipay","支付宝"),new LayUIData("alipayh5","支付宝H5"),new LayUIData("alipayCard","支付宝转卡"),new LayUIData("alipaysmTransfer","alipaysmTransfer"),new LayUIData("bankCard","银行卡bankCard"))),
    SEVENPAY("7付支付第三方平台","com.ra.service.channel.SevenPayInterface",types(new LayUIData("wechat","微信"),new LayUIData("alipay","支付宝"),new LayUIData("F2Fpay","当面付"))),
    KKone("KKone支付第三方平台","com.ra.service.channel.KKOneInterface",types(new LayUIData("ALIPAY","支付宝"),new LayUIData("WXPAY","微信"),new LayUIData("PDD","拼多多"),new LayUIData("JDWY","网银"),new LayUIData("YHK","银行卡"))),
    KUAIFAN("快帆支付第三方平台","com.ra.service.channel.UCPayInterface",types(new LayUIData("wechat","wechat"),new LayUIData("alipay","alipay"),new LayUIData("alipayh5","alipayh5"),new LayUIData("alipayIdXqd","alipayIdXqd"),new LayUIData("alipayTrans","alipayTrans"),new LayUIData("alipayIdTransfer","alipayIdTransfer"),new LayUIData("bankCard","bankCard"))),
    MEIYIN("美盈支付第三方平台","com.ra.service.channel.MeiYinPayInterface",types(new LayUIData("1","微信扫码"),new LayUIData("2","支付宝扫码"),new LayUIData("3","支付宝WAP"),new LayUIData("4","QQ扫码"),new LayUIData("5","QQWAP"),new LayUIData("6","微信WAP"))),
    BUBUGAO("步步高第三方平台","com.ra.service.channel.BuBuGaoInterface",types(new LayUIData("1","支付宝扫码"),new LayUIData("2","微信扫码"),new LayUIData("3","银行卡转账"),new LayUIData("4","微信转手机号"),new LayUIData("5","微信转银行卡"),new LayUIData("6","云闪付"),new LayUIData("11","吱口令"),new LayUIData("16","微信赞赏码"),new LayUIData("15","支付宝号转账"))),
    BEIDOUXING("北斗星第三方平台","com.ra.service.channel.BeiDouXingInterface",types(new LayUIData("alipay","支付宝扫码"),new LayUIData("wechat","微信"),new LayUIData("alipayaccount","支付宝转账"))),
    LIANMENG("联盟第三方平台","com.ra.service.channel.LianMengInterface",types(new LayUIData("1","微信扫码"),new LayUIData("2","支付宝扫码"),new LayUIData("5","支付宝H5转账"),new LayUIData("12","支付宝群红包"),new LayUIData("13","支付宝转银行卡"))),
    TENGLONGPAY("新腾龙支付第三方平台","com.ra.service.channel.TengLongPayInterface",types(new LayUIData("ALIPAY","支付宝"),new LayUIData("WEIXIN","微信"),new LayUIData("BANK","银行卡"))),
    XIANYU("咸鱼支付第三方平台","com.ra.service.channel.XianYuInterface",types(new LayUIData("YL","银联个码"),new LayUIData("WX","微信个码"),new LayUIData("ALI","支付宝个码"),new LayUIData("XY","支付宝唤醒"))),
    HAIYANG("海洋支付第三方平台","com.ra.service.channel.HaiYangInterface",types(new LayUIData("1","微信扫码"),new LayUIData("2","支付宝扫码或H5"),new LayUIData("3","银行卡转账"),new LayUIData("5","云闪付"),new LayUIData("7","支付宝转银行卡"))),
    RAYF("Rayf支付第三方平台","com.ra.service.channel.RayfPayInterface",types(new LayUIData("AG_GS_FACE","支付宝微信通用"))),
    ARAYPAY("ARAY支付第三方平台","com.ra.service.channel.ARYAPayInterface",types(new LayUIData("alipay_zk_h5","支付宝H5转卡(等待授权方式)"),new LayUIData("alipay_zk_cp","支付宝复制转卡"),new LayUIData("alipay_zk_fx","支付宝飞行模式转卡"))),
    ARYATAOBAO("ARYA支付淘宝红包","com.ra.service.channel.ARYAInterface",types(new LayUIData("alipay_wxhb","淘宝红包（500内）"))),
    DUBAI("迪拜支付第三方平台","com.ra.service.channel.DuBaiInterface",types(new LayUIData("1","支付宝扫码"),new LayUIData("2","微信扫码"),new LayUIData("3","银行卡转账"),new LayUIData("4","支付宝转卡"))),
    XIONGSHI("雄狮支付第三方平台","com.ra.service.channel.XiongShiInterface",types(new LayUIData("alipay_qrcode","支付宝扫码"),new LayUIData("alipay_account","支付宝转账"),new LayUIData("wechat_qrcode","微信扫码"))),
    BATIANXIA("霸天下支付第三方平台","com.ra.service.channel.BaTianXiaPayInterface",types(new LayUIData("998","支付宝扫码"),new LayUIData("999","支付宝WAP"),new LayUIData("994","微信H5"),new LayUIData("995","支付宝话费"),new LayUIData("996","银联网银"),new LayUIData("951","支付宝小额红包"),new LayUIData("997","支付宝转卡"))),
    YONGCHUAN("永川支付第三方平台","com.ra.service.channel.YongChuanPayInterface",types(new LayUIData("1004","支付宝话费30.50.100.200.300.500"),new LayUIData("1003","微信话费30.50.100.200.300.500"))),
    SJTWOSJ("SJ2SJ支付第三方平台","com.ra.service.channel.SJTwoSJInterface",types(new LayUIData("wxh5","微信H5"),new LayUIData("zfbh5","支付宝H5"),new LayUIData("icbc","中国工商银行"),new LayUIData("ccb","中国建设银行"),new LayUIData("abc","中国农业银行"),new LayUIData("psbc","中国邮政储蓄银行"),new LayUIData("comm","交通银行"),new LayUIData("cmb","招商银行"),new LayUIData("boc","中国银行"),new LayUIData("ceb","中国光大银行"),new LayUIData("citic","中信银行"),new LayUIData("spbd","浦发银行"),new LayUIData("cib","兴业银行"),new LayUIData("spabank","中国平安银行"),new LayUIData("hxbank","华夏银行"),new LayUIData("gdb","广发银行"),new LayUIData("aligateway","网关"))),
    DAJIDALI("大吉大利支付第三方平台","com.ra.service.channel.DaJiDaLiInterface",types(new LayUIData("1","支付宝"),new LayUIData("2","微信"),new LayUIData("icbc","中国工商银行"),new LayUIData("ccb","中国建设银行"),new LayUIData("abc","中国农业银行"),new LayUIData("psbc","中国邮政储蓄银行"),new LayUIData("comm","交通银行"),new LayUIData("cmb","招商银行"),new LayUIData("boc","中国银行"),new LayUIData("ceb","中国光大银行"),new LayUIData("citic","中信银行"),new LayUIData("spbd","浦发银行"),new LayUIData("cib","兴业银行"),new LayUIData("spabank","中国平安银行"),new LayUIData("hxbank","华夏银行"),new LayUIData("gdb","广发银行"))),
    FACAI("发财第三方平台","com.ra.service.channel.FaCaiInterface",types(new LayUIData("1","微信支付"),new LayUIData("2","支付宝"),new LayUIData("3","微信手机号转账"),new LayUIData("4","支付宝号转账"),new LayUIData("5","支付宝转账"),new LayUIData("10","支付宝转银行卡"),new LayUIData("11","网银转支付宝"))),
    HAIDAO("海盗支付第三方平台","com.ra.service.channel.HaiDaoInterface",types(new LayUIData("1","支付宝扫码"),new LayUIData("2","微信扫码"),new LayUIData("3","银行卡转账"),new LayUIData("4","支付宝转卡"))),
    WANGPAI("王牌支付第三方平台","com.ra.service.channel.WangPaiInterface",types(new LayUIData("1","支付宝扫码"),new LayUIData("2","微信扫码"),new LayUIData("3","银行卡转账"),new LayUIData("4","微信转手机号"),new LayUIData("5","微信转银行卡"),new LayUIData("6","云闪付"),new LayUIData("11","吱口令"),new LayUIData("12","支付宝转账码"),new LayUIData("14","支付宝固码")))
    ;
    @Getter
    private String title;

    @Getter
    private String className;

    @Getter
    private List<LayUIData> types;

    PayInterfaceEnum(String title,String className, List<LayUIData> types) {
        this.title=title;
        this.className= className;
        this.types=types;
    }

    private static List<LayUIData> types(LayUIData... types){
        List<LayUIData> list=new ArrayList<>();
        for (LayUIData s : types) {
            list.add(s);
        }
        return list;
    }
}

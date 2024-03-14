package com.ra.service.component;

import com.ra.common.utils.DateUtil;
import com.ra.common.utils.IpUtil;
import com.ra.common.utils.PropertyUtil;
import com.ra.dao.Enum.ConfigEnum;
import com.ra.dao.factory.ConfigFactory;
import com.ra.service.bean.req.QiNiuSettingReq;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * 文件上传组件
 *
 */
public final class UploadComponent {

    /**
     * 上传
     * @param file 文件数据
     * @param uploadType 上传类型
     * @return
     */
    public static String upload(MultipartFile file,UploadTypeEnum uploadType) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file is empty...");
        }

        String fileName = file.getOriginalFilename();  // 文件名
        String suffixName = fileName.substring(fileName.lastIndexOf("."));  // 后缀名
        uploadType.filterSuffix(suffixName);//过滤后缀
        uploadType.filterLimitSize(file.getSize());//过滤大小

        String path = PropertyUtil.getValue("spring.servlet.multipart.location") + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //生成新的文件名
        String destFileName = getDestFileName(file,uploadType);

        //如果是本地存储
        File dest = new File(path+File.separator+destFileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        file.transferTo(dest);//传输文件
        return destFileName;
    }

    /**
     * 目标文件名
     * @param typeEnum
     * @return
     */
    private static String getDestFileName(MultipartFile file,UploadTypeEnum typeEnum){
        String newFileName="";
        String fileName = "";
        String suffixName = "";
        String yyyyMMdd="";
        switch (typeEnum){
                case file:
                    newFileName=typeEnum.getDir()+File.separator+file.getOriginalFilename();//新文件名
                    break;
                case app:
                    newFileName=typeEnum.getDir()+File.separator+file.getOriginalFilename();//新文件名
                    break;
                case bulkText:
                    fileName = file.getOriginalFilename();  // 文件名
                    suffixName = fileName.substring(fileName.lastIndexOf("."));  // 后缀名
                    yyyyMMdd = DateUtil.dateToString(new Date(), "yyyyMMdd");
                    newFileName= typeEnum.getDir()+File.separator+yyyyMMdd + File.separator+"BULK"+System.currentTimeMillis() + suffixName;//新文件名
                    break;
                case pay:
                default:
                    fileName = file.getOriginalFilename();  // 文件名
                    suffixName = fileName.substring(fileName.lastIndexOf("."));  // 后缀名
                    yyyyMMdd = DateUtil.dateToString(new Date(), "yyyyMMdd");
                    newFileName= typeEnum.getDir()+File.separator+yyyyMMdd + File.separator+"IMG"+System.currentTimeMillis() + suffixName;//新文件名
                    break;
            }
            return newFileName;
    }
    /**
     * 获取服务器的文件地址
     * @param request Http请求
     * @return
     */
    public static String getHost(HttpServletRequest request) {
            return IpUtil.getHost(request)+File.separator+"static"+File.separator;
    }

    /**
     * 获取服务器的图片地址
     * @param request Http请求
     * @return
     */
    public static String getPayHost(HttpServletRequest request){
        return getHost(request);
    }

    /**
     * 获取服务器的图片地址
     * @param request Http请求
     * @return
     */
    public static String getCertificationHost(HttpServletRequest request){
        return getHost(request);
    }


    /**
     * 获取服务器的图片地址
     * @param request Http请求
     * @return
     */
    public static String getAvatarHost(HttpServletRequest request){
        return getHost(request);
    }


    /**
     * 获取服务器的APP下载地址
     * @param request Http请求
     * @return
     */
    public static String getAppHost(HttpServletRequest request){
        return getHost(request)+UploadTypeEnum.app.getDir();
    }

    /**
     * 获取登录高级验证身份图片的下载地址
     * @param request Http请求
     * @return
     */
    public static String getLoginVerifyHost(HttpServletRequest request){
        return getHost(request)+UploadTypeEnum.loginVerify.getDir();
    }

    /**
     * 获取服务器的文件地址
     * @param request Http请求
     * @return
     */
    public static String getFileHost(HttpServletRequest request){
        return getHost(request)+UploadTypeEnum.file.getDir();
    }

    /**
     * 文件类型枚举
     */
    public static enum UploadTypeEnum{

        //头像
        avatar("avatar",".jpg;.png;.jpeg",1024*1024*1),
        //付款凭证
        pay("pay",".jpg;.png;.jpeg",1024*1024*5),
        //身份认证
        certification("certification",".jpg;.png;.jpeg",1024*1024*5),
        //登录高级安全验证
        loginVerify("loginVerify",".jpg;.png;.jpeg",1024*1024*3),
        //文件
        file("file","*",1024*1024*100),
        //APP包
        app("apps", ".apk;.xml;.app;.ipa", 1024*1024*10),
        //富文本编辑
        ueditor("ueditor",".jpg;.png;.jpeg",1024*1024*3),
        //批量文本
        bulkText("bulkText",".xls;.xlsx",1024*1024*100);

        /**
         * 文件path
         */
        private String dir;

        /**
         * 文件后缀
         * 例如：.jpg .png 分号分隔 *代表不限制
         */
        private String suffix;

        /**
         * 限制大小
         */
        private long limitSize;

        public String getDir() {
            return dir;
        }

        public String getSuffix() {
            return suffix;
        }

        public long getLimitSize() {
            return limitSize;
        }

        UploadTypeEnum(String dir, String suffix,long linitSize){
            this.dir=dir;
            this.suffix=suffix;
            this.limitSize=linitSize;
        }

        /**
         * 过滤非法后缀
         */
        public void filterSuffix(String fileSuffx){
            String[] suffxs = getSuffix().split(";");
            for (String suffx : suffxs) {
                if(suffx.equals(fileSuffx.toLowerCase())){
                    return;
                }
            }
            throw new IllegalArgumentException("文件格式非法");
        }

        /**
         * 过滤文件大小
         */
        public void filterLimitSize(long fileSize){
            if(fileSize>getLimitSize()){
                throw new IllegalArgumentException("文件不得大于"+(getLimitSize()/1024)+" kb");
            }
        }
    }

}

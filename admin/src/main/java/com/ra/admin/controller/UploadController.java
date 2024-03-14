package com.ra.admin.controller;


import com.ra.service.component.UploadComponent;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 上传图片控制器
 */
@RestController
@RequestMapping("/private/upload")
public class UploadController {

    /**
     * 百度富文本控件
     * 图片上传
     * @param file
     * @return
     */
    @PostMapping(value = "/ueditor")
    public Object fileUpload(@RequestParam("file") MultipartFile file,HttpServletRequest request) throws IOException {
        Assert.notNull(file, "file is empty");

        String filePath = UploadComponent.upload(file, UploadComponent.UploadTypeEnum.ueditor);

        Map<String,Object> result=new HashMap<>();
        result.put("state","SUCCESS");
        result.put("url",UploadComponent.getHost(request)+ filePath);
        result.put("title",filePath);
        result.put("original",filePath);
        return result;
    }

    /**
     * 获取ueditor配置
     * @param request
     * @param response
     * @return
     */
    @GetMapping(value = "/ueditor")
    public String ueditor(HttpServletRequest request, HttpServletResponse response) {

        String s = "{\n"+
                "            \"imageActionName\": \"uploadimage\",\n" +
                "                \"imageFieldName\": \"file\", \n" +
                "                \"imageMaxSize\": 2048000, \n" +
                "                \"imageAllowFiles\": [\".png\", \".jpg\", \".jpeg\", \".gif\", \".bmp\"], \n" +
                "                \"imageCompressEnable\": true, \n" +
                "                \"imageCompressBorder\": 1600, \n" +
                "                \"imageInsertAlign\": \"none\", \n" +
                "                \"imageUrlPrefix\": \"\",\n" +
                "                \"imagePathFormat\": \"/ueditor/jsp/upload/image/{yyyy}{mm}{dd}/{time}{rand:6}\" }";
        return s;

    }
}

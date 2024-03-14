package com.ra.admin.utils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * @author huli
 * @date 2018/12/12  15:06
 * @description
 */
public class IosXmlUtils {

    public static String createPlist(String url, String userName, String versionId, String title, String filePath, String fileName) throws IOException {
        //这个地址应该是创建的服务器地址，在这里用生成到本地磁盘地址
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        final String PLIST_PATH = filePath + fileName;
        file = new File(PLIST_PATH);
        if (file.exists()) {
            file.delete();
        } else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String plist = getString(url, userName, versionId, title);
        try {
            FileOutputStream output = new FileOutputStream(file);
            OutputStreamWriter writer;
            writer = new OutputStreamWriter(output, "UTF-8");
            writer.write(plist);
            writer.close();
            output.close();
        } catch (Exception e) {
            System.err.println("==========创建plist文件异常：" + e.getMessage());
        }
        return fileName;
    }

    @NotNull
    private static String getString(String url, String userName, String versionId, String title) {

        StringBuilder stringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        stringBuilder.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        stringBuilder.append("<plist version=\"1.0\">\n");
        stringBuilder.append("<dict>\n");
        stringBuilder.append("  <key>items</key>\n");
        stringBuilder.append("      <array>\n");
        stringBuilder.append("          <dict>\n");
        stringBuilder.append("              <key>assets</key>\n");
        stringBuilder.append("                  <array>\n");
        stringBuilder.append("                      <dict>\n");
        stringBuilder.append("                          <key>kind</key>\n");
        stringBuilder.append("                          <string>software-package</string>\n");
        stringBuilder.append("                          <key>url</key>\n");
        stringBuilder.append("                          <string>");
        stringBuilder.append(url);
        stringBuilder.append("</string>\n");
        stringBuilder.append("                      </dict>\n");
        stringBuilder.append("                  </array>\n");
        stringBuilder.append("                  <key>metadata</key>\n");
        stringBuilder.append("                  <dict>\n");
        stringBuilder.append("                      <key>bundle-identifier</key>\n");
        stringBuilder.append("                      <string>");
        stringBuilder.append(userName);
        stringBuilder.append("</string>\n");
        stringBuilder.append("                      <key>bundle-version</key>\n");
        stringBuilder.append("                      <string>");
        stringBuilder.append(versionId);
        stringBuilder.append("</string>\n");
        stringBuilder.append("                      <key>kind</key>\n");
        stringBuilder.append("                      <string>software</string>\n");
        stringBuilder.append("                      <key>title</key>\n");
        stringBuilder.append("                      <string>");
        stringBuilder.append(title);
        stringBuilder.append("</string>\n");
        stringBuilder.append("                  </dict>\n");
        stringBuilder.append("         </dict>\n");
        stringBuilder.append("      </array>\n");
        stringBuilder.append("</dict>\n");
        stringBuilder.append("</plist>");

        return stringBuilder.toString();
    }


}

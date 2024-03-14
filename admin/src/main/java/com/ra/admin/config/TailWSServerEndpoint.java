package com.ra.admin.config;

import com.ra.admin.utils.TailLogThread;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.yeauty.annotation.*;
import org.yeauty.pojo.ParameterMap;
import org.yeauty.pojo.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Websocket端点
 */
@ServerEndpoint(value = "/log",port = 8887)
@Component
public class TailWSServerEndpoint {

    private static final Logger logger= LoggerFactory.getLogger(TailWSServerEndpoint.class);
    private Process process;
    private InputStream inputStream;

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, ParameterMap parameterMap) throws IOException {
        logger.info("one connection open:"+session.id().asLongText());
        try{
            String dir=parameterMap.getParameter("dir");
            if(StringUtils.isEmpty(dir)){
                dir="/opt/admin/info.log";
            }else if(dir.equalsIgnoreCase("open")){
                dir="/opt/open/info.log";
            }else{
                dir="/opt/admin/info.log";
            }

            //查看日志是否存在
            File file = new File(dir);
            if(file.exists()){
                session.sendText("等待载入文件......" + "<br>");
                // 执行tail -f命令
                process = Runtime.getRuntime().exec("tail -f "+dir);
                inputStream = process.getInputStream();
                // 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
                TailLogThread thread = new TailLogThread(inputStream, session);
                thread.start();
            }else{
                session.sendText("<a style='color: #ff0000'>文件没有找到</a>" + "<br>");
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            try {
                session.sendText(e.getMessage() + "<br>");
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
            }
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        logger.info("one connection closed:"+session.id().asLongText());
        try {
            if(inputStream != null)
                inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(process != null)
            process.destroy();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
    }

    /**
     * 收到消息
     * @param session
     * @param message
     */
    @OnMessage
    public void OnMessage(Session session, String message) {
    }
}
package com.ra.admin.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yeauty.pojo.Session;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TailLogThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(TailLogThread.class);

    private BufferedReader reader;
    private Session session;

    public TailLogThread(InputStream in, Session session) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.session = session;

    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                // 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
                session.sendText(line + "<br>");

                try {
                    Thread.sleep(1);
                }catch (Exception e){}
            }
        } catch (EOFException e1) {
            try {
                session.sendText("客户端已经关闭!" + "<br>");
            } catch (Exception e) {
                logger.error("服务流关闭提示消息发送报错：" + e.getMessage(), e);
            }
            logger.info("出现了EOFExcption:：服务流已经关闭!");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
package com.ra.common.utils;


import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpUtil {

    private final static Logger logger=LoggerFactory.getLogger(HttpUtil.class);
    private static PoolingHttpClientConnectionManager connectionManager;
    private static RequestConfig requestConfig;
    //连接超时7秒
    private static final int MAX_TIMEOUT = 7000;

    static {
        // 创建连接池
        connectionManager = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connectionManager.setMaxTotal(50);
        // 将每个路由基础的连接增加到getMaxTotal()
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        // 可用空闲连接过期时间,重用空闲连接时会先检查是否空闲时间超过这个时间，如果超过，释放socket重新建立
        connectionManager.setValidateAfterInactivity(10000);
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时
        configBuilder.setConnectTimeout(MAX_TIMEOUT);
        // 设置读取超时
        configBuilder.setSocketTimeout(MAX_TIMEOUT);
        // 设置从连接池获取连接实例的超时
        configBuilder.setConnectionRequestTimeout(MAX_TIMEOUT);

        requestConfig = configBuilder.build();
    }

    /**
     * 发送 GET 请求
     * 
     * @param url
     * @return
     */
    public static String doGet(String url) {
        return doGet(url, new HashMap<String, Object>());
    }

    /**
     * 发送 GET 请求
     * 
     * @param url
     * @param params
     *            请求参数
     * @return
     */
    public static String doGet(String url, Map<String, Object> params) {
        return doGet(url,params,new HashMap<>());
    }


    /**
     * 发送 GET 请求
     *
     * @param url
     * @param params
     *            请求参数
     * @return
     */
    public static String doGet(String url, Map<String, Object> params,Map<String,String> header) {

        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            if(sb.length() > 0)
                sb.append("&");
            else
                sb.append("?");
            sb.append(key).append("=").append(params.get(key));
        }
        url += sb.toString();
        String result = null;// 返回的数据
        HttpClient httpClient = null;
        if (url.startsWith("https")) {// 创建https连接
            httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                    .setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
        } else {// 创建http连接
            httpClient = HttpClients.createDefault();
        }
        HttpResponse response=null;
        try {
            HttpGet httpGet = new HttpGet(url);

            for (String key : header.keySet()) {
                httpGet.setHeader(key,header.get(key));
            }

            response = httpClient.execute(httpGet);
            // 获取返回的内容
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            e.printStackTrace();
        }finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }



    /**
     * 发送 POST 请求
     * 
     * @param url
     * @return
     */
    public static String doPOST(String url) {
        return doPOST(url, new HashMap<String, Object>());
    }

    /**
     * 发送 POST 请求，map
     * 
     * @param url
     * @param params
     *            请求参数
     * @return
     */
    public static String doPOST(String url, Map<String, Object> params) {
        CloseableHttpClient httpClient = null;
        if (url.startsWith("https")) {// 创建https连接
            httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                    .setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
        } else {// 创建http连接
            httpClient = HttpClients.createDefault();
        }
        String httpStr = null;
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;

        try {

            httpPost.setConfig(requestConfig);
            List<NameValuePair> pairList = new ArrayList<>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value=entry.getValue()==null?null:entry.getValue().toString();
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), value);
                pairList.add(pair);
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if(entity!=null){
                httpStr = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            e.printStackTrace();
        }finally {
            if (response != null) {  
                try {  
                    EntityUtils.consume(response.getEntity());  
                } catch (IOException e) {  
                    e.printStackTrace();
                }  
            }  
        }
        return httpStr;
    }


    /**
     * 发送 POST 请求，JSON形式
     *
     * @param apiUrl
     * @param json
     *            json对象
     * @return
     */
    public static String doPOST(String apiUrl, Object json) {
        return doPOST(apiUrl,json,new HashMap<>());
    }

     /** 
     * 发送 POST 请求，JSON形式 
     *  
     * @param apiUrl 
     * @param json 
     *            json对象 
     * @return 
     */  
    public static String doPOST(String apiUrl, Object json,Map<String,String> header) {
        CloseableHttpClient httpClient = null;  
        if (apiUrl.startsWith("https")) {  
            httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())  
                    .setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();  
        } else {  
            httpClient = HttpClients.createDefault();  
        }  
        String httpStr = null;  
        HttpPost httpPost = new HttpPost(apiUrl);  
        CloseableHttpResponse response = null;  

        try {  
            httpPost.setConfig(requestConfig);  
            StringEntity stringEntity = new StringEntity(json.toString(), "UTF-8");// 解决中文乱码问题  
            stringEntity.setContentEncoding("UTF-8");  
            stringEntity.setContentType("application/json");  
            httpPost.setEntity(stringEntity);

            for (String key : header.keySet()) {
                httpPost.setHeader(key,header.get(key));
            }

            response = httpClient.execute(httpPost);  
            HttpEntity entity = response.getEntity();  
            httpStr = EntityUtils.toString(entity, "UTF-8");  
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            e.printStackTrace();  
        } finally {  
            if (response != null) {  
                try {  
                    EntityUtils.consume(response.getEntity());  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
        return httpStr;  
    }  

    /**
     * 创建SSL安全连接
     * 
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (GeneralSecurityException e) {
            logger.error(e.getMessage(),e);
            e.printStackTrace();
        }
        return sslsf;
    }

}
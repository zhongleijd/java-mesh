package com.huawei.test.configelement.service.impl;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 功能描述：HttpClient客户端请求
 *
 * @author hjw
 * @since 2022-01-10
 */
public class HttpClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientService.class);
    private static final HttpClientBuilder httpClientBuilder = HttpClients.custom();
    private static CloseableHttpClient closeableHttpClient;
    private static final int maxTotal = 50;
    private static final int defaultMaxPerRoute = 50;
    private static final int connectTimeout = 3000;
    private static final int socketTimeout = 5000;
    private static final int connectionRequestTimeout = 3000;

    public HttpClientService() {}

    static {
        /*
         * 绕过不安全的https请求的证书验证
         */
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", trustHttpCertificates())
                .build();
        /*
         * 创建连接池对象
         */
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        // 连接池最大有50个连接，<=20
        cm.setMaxTotal(maxTotal);
        // 每个路由默认有多少连接,<=2
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        httpClientBuilder.setConnectionManager(cm);
        /*
         * 设置请求的默认配置
         */
        RequestConfig requestConfig = RequestConfig.custom()
                // 连接超时，完成tcp 3次握手的时间上限
                .setConnectTimeout(connectTimeout)
                // 读取超时，表示从请求的网址处获得响应数据的时间间隔
                .setSocketTimeout(socketTimeout)
                // 同连接池连接的超时时间
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        /*
         * 设置默认保持长连接
         */
        List<Header> defaultHeaders = new ArrayList<>();
        BasicHeader basicHeader = new BasicHeader("Connection", "keep-alive");
        defaultHeaders.add(basicHeader);
        httpClientBuilder.setDefaultHeaders(defaultHeaders);
    }

    /**
     * 发送get请求
     * @param url 请求url，参数需经过URLEncode编码处理
     * @param headers 自定义请求头
     * @return 返回结果
     */
    public static HttpResponse executeGet(String url, Map<String, String> headers, int connectTimeout, int socketTimeout){
        closeableHttpClient = httpClientBuilder.build();
        HttpGet httpGet = new HttpGet(url);
        if (headers != null){
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for(Map.Entry<String, String> entry:entries){
               httpGet.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        CloseableHttpResponse response = null;
        try {
            response = closeableHttpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            if (HttpStatus.SC_OK == statusLine.getStatusCode()){
                return response;
            }else {
                LOGGER.error("The http response is error:" + statusLine.getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送表单类型的post请求
     * @param url 要请求的url
     * @param param 参数列表
     * @param headers 自定义头
     * @return 返回结果
     */
    public static HttpResponse postForm(String url, List<NameValuePair> param, Map<String, String> headers, int connectTimeout, int socketTimeout){
        closeableHttpClient = httpClientBuilder.build();
        HttpPost httpPost = new HttpPost(url);
        if (headers != null){
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for(Map.Entry<String, String> entry:entries){
                httpPost.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(socketTimeout)
            .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(param, Consts.UTF_8);
        httpPost.setEntity(formEntity);

        return getHttpResponse(closeableHttpClient, httpPost);
    }

    /**
     * 发送json类型的post请求
     * @param url 要请求的url
     * @param body 参数列表
     * @param headers 自定义头
     * @return 返回结果
     */
    public static HttpResponse postJson(String url, String body, Map<String, String> headers){
        closeableHttpClient = httpClientBuilder.build();
        HttpPost httpPost = new HttpPost(url);
        if (headers != null){
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for(Map.Entry<String, String> entry:entries){
                httpPost.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        httpPost.addHeader("Content-Type", "application/json;charset=utf-8");
        StringEntity jsonEntity = new StringEntity(body, Consts.UTF_8);
        jsonEntity.setContentEncoding(Consts.UTF_8.name());
        httpPost.setEntity(jsonEntity);

        return getHttpResponse(closeableHttpClient, httpPost);
    }

    public static HttpResponse getHttpResponse(CloseableHttpClient closeableHttpClient, HttpPost httpPost){
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            if (HttpStatus.SC_OK == statusLine.getStatusCode()){
                return response;
            }else {
                LOGGER.info("The response is error:" + statusLine.getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建支持安全协议的工程
     * @return ConnectionSocketFactory
     */
    private static ConnectionSocketFactory trustHttpCertificates() {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        try {
            sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });

            SSLContext sslContext = sslContextBuilder.build();
            return new SSLConnectionSocketFactory(sslContext,
                    new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"}
                    ,null, NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct secure connection factory");
        }
    }

    /**
     * 释放资源
     * @param response the HttpResponse to release resources, may be null or already closed.
     *
     */
    private static void closeQuietly(final CloseableHttpResponse response) {
        if (response != null){
            try{
                try {
                    EntityUtils.consume(response.getEntity());
                } finally {
                    response.close();
                }
            }catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}

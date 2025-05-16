package com.hydra.websocket.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;


/**
 * http请求工具类
 * @author tangjx
 *
 */
public class HttpUtils {
    private final static String CTYPE_FORM = "application/x-www-form-urlencoded;charset=utf-8";
    private final static String CTYPE_JSON = "application/json; charset=utf-8";
    private final static String charset = "utf-8";

    /*private static HttpUtils instance = null;

    public static HttpUtils getInstance() {
        if (instance == null) {
            return new HttpUtils();
        }
        return instance;
    }*/

    private static class DefaultTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
    }

    /**
     * 以application/json; charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static  String sendPostJson(String url, String jsonContent){
        return doRequest("POST", url, jsonContent, 60000, 60000, CTYPE_JSON,
                null);
    }

    /**
     * 以application/json; charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static  String sendPostJson(String url,  Map<String, Object> params){
    	JSONObject jsonObj = new JSONObject(params);
        return doRequest("POST", url, jsonObj.toString(), 15000, 15000, CTYPE_JSON,
                null);
    }
    
    
    /**
     * POST 以application/x-www-form-urlencoded;charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static String sendPostForm(String url){
        return doRequest("POST", url, "", 15000, 15000, CTYPE_FORM, null);
    }

    /**
     * POST 以application/x-www-form-urlencoded;charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static String sendPostForm(String url, Map<String, String> params){
        return doRequest("POST", url, buildQuery(params), 15000, 15000,
                CTYPE_FORM, null);
    }

    /**
     * POST 以application/x-www-form-urlencoded;charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static String sendGetForm(String url) {
        return doRequest("GET", url, "", 15000, 15000, CTYPE_FORM, null);
    }

    /**
     * POST 以application/x-www-form-urlencoded;charset=utf-8方式传输
     * 
     * @param url
     * @param requestContent
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public static String sendGgetForm(String url, Map<String, String> params){
        return doRequest("GET", url, buildQuery(params), 15000, 15000,
                CTYPE_FORM, null);
    }

    /**
     * 以POST方式上传文件
     *
     * @param requestUrl
     * @param filePath
     * @return
     */
    public static String fileUpload(String requestUrl, String filePath) {
        File file = new File(filePath);
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
//        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            // 允许Input、Output，不使用Cache
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            // 设置以POST方式进行传送
            con.setRequestMethod("POST");
            // 设置RequestProperty
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);
            // 构造DataOutputStream流
            DataOutputStream ds = new DataOutputStream(con.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"file\";filename=\"" + file.getName() + "\"" + end);
            ds.writeBytes(end);
            // 构造要上传文件的FileInputStream流
            FileInputStream fis = new FileInputStream(file);
            // 设置每次写入1024bytes
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            // 从文件读取数据至缓冲区
            while ((length = fis.read(buffer)) != -1) {
                // 将资料写入DataOutputStream中
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
            // 关闭流
            fis.close();
            ds.flush();
            // 获取响应流
            InputStream is = con.getInputStream();
            String res = getStreamAsString(is,charset, con);
            // 关闭DataOutputStream
            ds.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param urlStr
     * @param params
     * @param filePath
     */
    public static String fileDownloadPost(String urlStr, Map<String, Object> params, String filePath){
        String rsp;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept",
                    "text/xml,text/javascript,text/html,application/json");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            JSONObject jsonObj = new JSONObject(params);
            String requestContent = jsonObj.toString();
            OutputStream out = null;
            if(!StringUtils.isEmpty(requestContent)){
                out = conn.getOutputStream();
                out.write(requestContent.getBytes("utf-8"));
            }
            //得到输入流
            inputStream = conn.getInputStream();
            //获取自己数组
            byte[] getData = readInputStream(inputStream);
            String fileName = new String(conn.getHeaderField("Content-Disposition").getBytes("ISO-8859-1"), "UTF-8");
            fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename=") + 9),"UTF-8");
            if (StringUtils.isNotEmpty(fileName)) {
                File file = new File(filePath + File.separator + fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(getData);
                if(fos!=null){
                    fos.close();
                }
            }
            rsp = conn.getHeaderField("Data-Content");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("下载失败!");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rsp;
    }

    /**
     * 从输入流中获取字节数组
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();

    }

    private static String doRequest(String method, String url, String requestContent,
            int connectTimeout, int readTimeout, String ctype,
            Map<String, String> headerMap){
        HttpURLConnection conn = null;
        OutputStream out = null;
        String rsp = null;
        try {
            conn = getConnection(new URL(url), method, ctype, headerMap);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            
            if(!StringUtils.isEmpty(requestContent)){
                out = conn.getOutputStream();
                out.write(requestContent.getBytes(charset));
            }
            
            rsp = getResponseAsString(conn);
        } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (out != null) {
                try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
            if (conn != null) {
                conn.disconnect();
            }
            conn = null;
        }
        return rsp;
    }

    private static HttpURLConnection getConnection(URL url, String method,
            String ctype, Map<String, String> headerMap) throws IOException{
        HttpURLConnection conn;
        if ("https".equals(url.getProtocol())) {
            SSLContext ctx;
            try {
                ctx = SSLContext.getInstance("TLS");
                ctx.init(new KeyManager[0],
                        new TrustManager[] { new DefaultTrustManager() },
                        new SecureRandom());
            } catch (Exception e) {
                throw new IOException(e);
            }
            HttpsURLConnection connHttps = (HttpsURLConnection) url
                    .openConnection();
            connHttps.setSSLSocketFactory(ctx.getSocketFactory());
            connHttps.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            conn = connHttps;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept",
                "text/xml,text/javascript,text/html,application/json");
        conn.setRequestProperty("Content-Type", ctype);
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return conn;
    }

    private static String getResponseAsString(HttpURLConnection conn)
            throws IOException {
        InputStream es = conn.getErrorStream();
        if (es == null) {
        	try{
        		return getStreamAsString(conn.getInputStream(), charset, conn);
        	}catch (Exception e) {
        		
			}
        	return null;
        } else {
            String msg = getStreamAsString(es, charset, conn);
            if (StringUtils.isEmpty(msg)) {
                throw new IOException(conn.getResponseCode() + ":"
                        + conn.getResponseMessage());
            } else {
                return msg;
            }
        }
    }

    private static String getStreamAsString(InputStream stream, String charset,
            HttpURLConnection conn) throws IOException {
        try {
            Reader reader = new InputStreamReader(stream, charset);

            StringBuilder response = new StringBuilder();
            final char[] buff = new char[1024];
            int read = 0;
            while ((read = reader.read(buff)) > 0) {
                response.append(buff, 0, read);
            }

            return response.toString();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static String buildQuery(Map<String, String> params){
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder query = new StringBuilder();
        Set<Map.Entry<String, String>> entries = params.entrySet();
        boolean hasParam = false;

        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (hasParam) {
                query.append("&");
            } else {
                hasParam = true;
            }
            try {
				query.append(name).append("=")
				        .append(URLEncoder.encode(value, charset));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }
        return query.toString();
    }
}
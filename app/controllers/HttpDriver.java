package controllers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;  

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;


public class HttpDriver {

	private static final CloseableHttpClient httpClient;
	public static final String CHARSET = "UTF-8";
	private static  CloseableHttpClient httpsClient;
	
	
	
	static {
		RequestConfig config = RequestConfig.custom().setConnectTimeout(60000)
				.setSocketTimeout(15000).build();
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config)
				.build();
		
		//httpsClient= HttpClientBuilder.create().setDefaultRequestConfig(config)
			//	.build();
		try{
			httpsClient=createHttpsClient();
		}catch(Exception e){

		}
	}
	
	
	 
	public static CloseableHttpClient createHttpsClient() throws NoSuchAlgorithmException, KeyManagementException {
	    X509TrustManager x509mgr = new X509TrustManager() {
	        @Override
	        public void checkClientTrusted(X509Certificate[] xcs, String string) {
	        }
	        @Override
	        public void checkServerTrusted(X509Certificate[] xcs, String string) {
	        }
	        @Override
	        public X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	    };
	 
	    SSLContext sslContext = SSLContext.getInstance("SSL");
	    sslContext.init(null, new TrustManager[] { x509mgr }, null);
	    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	 
	    return HttpClients.custom().setSSLSocketFactory(sslsf).build();
	}

	public static String doGet(String url, Map<String, String> params) throws Exception {
		//logger.info("- [get] - URL:" + url);
		
		return doGet(url, params, CHARSET,false);
	}

	public static String doPost(String url, Map<String, String> params) throws Exception {
		return doPost(url, params, CHARSET,false,false);
	}
	
	public static String doPost(String url, Map<String, String> params,boolean multipart) throws Exception {
		return doPost(url, params, CHARSET,multipart,false);
	}
	
	public static String doPost(String url, Boolean bHttps, Map<String, String> params) throws Exception {
		return doPost(url, params, CHARSET,false,bHttps);
	}
	
	public static String doGet(String url, Map<String, String> params,Boolean bHttps) throws Exception {
		return doGet(url, params, CHARSET,bHttps);
	}
	
	
	public static CloseableHttpClient getHttpClient(boolean bHttps){
		if(bHttps){
			return httpsClient;
		}
		else {
			return httpClient;
		}
	}
	
	
	 /**
     * HTTP Get 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
     * @param charset    编码格式
     * @return    页面内容
	 * @throws Exception 
     */
	public static String doGet(String url, Map<String, String> params,
			String charset,boolean bHttps) throws Exception {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		

		try {
				if (params != null && !params.isEmpty()) {
					List<NameValuePair> pairs = new ArrayList<NameValuePair>(
							params.size());
					for (Map.Entry<String, String> entry : params.entrySet()) {
						String value = entry.getValue();
						if (value != null) {
							pairs.add(new BasicNameValuePair(entry.getKey(), value));
						}
					}
					url += "?"
							+ EntityUtils.toString(new UrlEncodedFormEntity(pairs,
									charset));
				}
			

			url = url.replace("%25","%");
			HttpGet httpGet = new HttpGet(url);
			CloseableHttpResponse response = getHttpClient(bHttps).execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpGet.abort();
				throw new RuntimeException("HttpClient,error status code :"
						+ statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, charset);
			}
			//EntityUtils.consume(entity);
			response.close();

			return result;
		} catch (Exception e) {
			throw e;
			/*if (e instanceof RequestAbortedException
					|| e instanceof SocketTimeoutException
					|| e instanceof SocketException) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return doGet(url, params, charset);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			String exception = baos.toString();
			logger.error(" - [LOG_EXCEPTION] - " + exception);
			e.printStackTrace();*/
		}
	}

	/**
     * HTTP Post 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
     * @param charset    编码格式
     * @param multipart  是否使用multipart方式传值
     * @return    页面内容
	 * @throws Exception 
     */
	public static String doPost(String url, Map<String, String> params,
			String charset,boolean multipart,boolean bHttps) throws Exception {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		

		try {
			
			HttpEntity reEntity =null;
			if(multipart){
				MultipartEntityBuilder builder=MultipartEntityBuilder.create();
				//builder.setCharset(Charset.forName(charset));
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

				ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, Charset.forName(charset));

				for(Map.Entry<String, String> entry: params.entrySet()){
					String value = entry.getValue();
					if (value != null) {
						//StringBody stringBody = new StringBody(value,contentType);
						//builder.addPart(entry.getKey(), stringBody);
						builder.addTextBody(entry.getKey(), value,contentType);
						//builder.addPart(entry.getKey(), new StringBody(value, contentType));
					}
				}
				
				reEntity=builder.build();
			}
			else{
				List<NameValuePair> pairs = null;
				if (params != null && !params.isEmpty()) {
					pairs = new ArrayList<NameValuePair>(params.size());
					for (Map.Entry<String, String> entry : params.entrySet()) {
						String value = entry.getValue();
						if (value != null) {
							pairs.add(new BasicNameValuePair(entry.getKey(), value));
						}
					}
					
					if(pairs.size()>0){
						reEntity= new UrlEncodedFormEntity(pairs, charset);
					}
				}
			}

			

			HttpPost httpPost = new HttpPost(url);
			if (reEntity!=null) {
				httpPost.setEntity(reEntity);
			}
			
			
			CloseableHttpResponse response = getHttpClient(bHttps).execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :"
						+ statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, charset);
			}
			//EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			throw e;
			/*if (e instanceof RequestAbortedException
					|| e instanceof SocketTimeoutException
					|| e instanceof SocketException) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//return doPost(url, params, charset,multipart);
			}
			e.printStackTrace();*/
		}
		//return null;
	}
}

package com.mama100.android.member.http;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.mama100.android.member.util.LogUtils;

/**
 * HTTP客户端数据通信核心类 此对象可以只创建一次。
 * 
 * @author jimmy
 */
public class CoreHttpClient {

	private static final String LOG_TAG = CoreHttpClient.class.getSimpleName();

	/** OK: Success! */
	public static final int OK = 200;

	/** Not Modified: There was no new data to return. */
	public static final int NOT_MODIFIED = 304;

	/**
	 * Bad Request: The request was invalid. An accompanying error message will
	 * explain why. This is the status code will be returned during rate
	 * limiting.
	 */
	public static final int BAD_REQUEST = 400;

	/** Not Authorized: Authentication credentials were missing or incorrect. */
	public static final int NOT_AUTHORIZED = 401;

	/**
	 * Forbidden: The request is understood, but it has been refused. An
	 * accompanying error message will explain why.
	 */
	public static final int FORBIDDEN = 403;

	/**
	 * Not Found: The URI requested is invalid or the resource requested, such
	 * as a user, does not exists.
	 */
	public static final int NOT_FOUND = 404;

	/**
	 * Not Acceptable: Returned by the Search API when an invalid format is
	 * specified in the request.
	 */
	public static final int NOT_ACCEPTABLE = 406;

	/**
	 * Internal Server Error: Something is broken. Please post to the group so
	 * the team can investigate.
	 */
	public static final int INTERNAL_SERVER_ERROR = 500;

	/** Bad Gateway: is down or being upgraded. */
	public static final int BAD_GATEWAY = 502;

	/**
	 * Service Unavailable: The servers are up, but overloaded with requests.
	 * Try again later. The search and trend methods use this to indicate when
	 * you are being rate limited.
	 */
	public static final int SERVICE_UNAVAILABLE = 503;

	//连接超时
	private static final int CONNECTION_TIMEOUT_MS = 10 * 1000;
	//传输超时
	private static final int SOCKET_TIMEOUT_MS = 30 * 1000;

	public static final int RETRIEVE_LIMIT = 20;
	public static final int RETRIED_TIME = 3;

	private DefaultHttpClient httpClient;
	
	//private static DefaultHttpClient httpsClient;
	private DefaultHttpClient httpsClient;

	public CoreHttpClient() {
		initHttpClient();
		
		httpsClient=httpClient; //暂时让两者相同，待确定添加Https验证的时候再修改
//		if(httpsClient==null)
//			initHttpsClient();
	}

	// 对外提供HttpClient实例
	/*
	 * public HttpClient getHttpClient() {
	 * 
	 * if (httpClient == null) { httpClient = prepare(); }
	 * 
	 * return httpClient; }
	 */

	/**
	 * Setup DefaultHttpClient
	 * 
	 * Use ThreadSafeClientConnManager.
	 * 
	 */
	/*
	 * private void prepare() {
	 * 
	 * // Create and initialize HTTP parameters HttpParams params = new
	 * BasicHttpParams(); ConnManagerParams.setMaxTotalConnections(params, 10);
	 * HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	 * 
	 * // Create and initialize scheme registry SchemeRegistry schemeRegistry =
	 * new SchemeRegistry(); schemeRegistry.register(new Scheme("http",
	 * PlainSocketFactory .getSocketFactory(), 80)); schemeRegistry.register(new
	 * Scheme("https", SSLSocketFactory .getSocketFactory(), 443));
	 * 
	 * // Create an HttpClient with the ThreadSafeClientConnManager.
	 * ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager( params,
	 * schemeRegistry); httpClient = new DefaultHttpClient(cm, params);
	 * 
	 * //httpClient = new DefaultHttpClient(params);
	 * 
	 * // TODO: need to release this connection in httpRequest() //
	 * cm.releaseConnection(conn, validDuration, timeUnit); //
	 * httpclient.getConnectionManager().shutdown();
	 * 
	 * }
	 */

	public void initHttpClient() {

		// Create and initialize HTTP parameters
		HttpParams params = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(params, 10);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		httpClient = new DefaultHttpClient(params);

	}
	
	public void initHttpsClient() {
		httpsClient=(DefaultHttpClient) SSLSocketFactoryEx.getmemberHttpsClient();
	}

	/**
	 * GET方式提交数据
	 * 
	 * @param url
	 *            http url, 如:http://hyt.mamam100.com/user.action?id=1
	 * @return
	 * @throws HttpException
	 */
	public HttpResponse get(String url) throws HttpException,
			SocketTimeoutException {
		return get(url, null);
	}

	/**
	 * GET方式提交数据
	 * 
	 * @param url
	 *            http url, 如:http://hyt.mamam100.com/user.action
	 * @param params
	 *            (key,value) 类似map的结构
	 * @return
	 * @throws HttpException
	 */
	public HttpResponse get(String url, List<BasicNameValuePair> params)
			throws HttpException, SocketTimeoutException {

		LogUtils.logd(LOG_TAG, "get " + " request to " + url);

		HttpResponse response = null;

		// Execute Request
		try {

			if (params != null && !params.isEmpty()) {
				// 格式化参数 变成URL的形式. 如： username=jimmy&password=123456
				String subUrlParamStr = URLEncodedUtils.format(params,
						HTTP.UTF_8);
				if (!url.contains("?")) {
					url += "?" + subUrlParamStr;
				} else {
					url += "&" + subUrlParamStr;
				}
			}

			URI uri = createURI(url);
			HttpGet request = new HttpGet(uri);

			// Setup ConnectionParams, Request Headers
			setupHTTPConnectionParams(request);

			//判断是http请求还是https请求
			if(uri.getScheme().toLowerCase().equals("https")){
				response=httpsClient.execute(request);
			}
			else
				response = httpClient.execute(request);

		} catch (ClientProtocolException e) {
			throw new HttpException(e.getMessage(), e);
		} catch (IOException ioe) {
			throw new HttpException(ioe.getMessage(), ioe);
		}

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			// It will throw a Exception while status code is not 200
			handleResponseStatusCode(statusCode);
		} else {
			LogUtils.loge(LOG_TAG, "response is null");
		}

		return response;
	}

	/**
	 * POST方式提交数据
	 * 
	 * @param url
	 *            http url, 如:http://hyt.mamam100.com/user.action
	 * @param params
	 *            (key,value) 类似map的结构
	 * @return HttpResponse
	 * @throws HttpException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse post(String url, List<BasicNameValuePair> params)
			throws Exception {
		return post(url, params, null);
	}

	/**
	 * POST方式提交数据
	 * 
	 * @param url
	 * @param params
	 * @param cookies
	 *            带上cookies
	 * @return
	 * @throws Exception
	 */
	public HttpResponse post(String url, List<BasicNameValuePair> params,
			Map<String, String> cookies) throws Exception {

		LogUtils.logd(LOG_TAG, "post " + " request to " + url);

		URI uri = createURI(url);

		HttpResponse response = null;

		HttpPost request = new HttpPost(uri);

		// Execute Request
		try {

			HttpEntity httpEntity = null;
			httpEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
			request.setEntity(httpEntity);

			// Setup ConnectionParams, Request Headers
			setupHTTPConnectionParams(request);

			// 加cookies
			// 方法一，post里面设置头文件，在Header里加Cookie
			if (cookies != null && !cookies.isEmpty()) {
				// if (!cookies.isEmpty()) {
				String cookiesStr = buildCookiesStr4Request(cookies);
				// request.addHeader("cookie", cookiesStr);
				request.addHeader("Cookie", cookiesStr);
//				Header[] headers = request.getAllHeaders();
//				for (Header header : headers) {
//					LogUtils.logd(LOG_TAG, header.toString());
//				}
			}

			// 设置SessionId方法二， cookie里面设置字段
			// cookieStore = (CookieStore)
			// localContext.getAttribute(ClientContext.COOKIE_STORE);
			// BasicClientCookie basicClientCookie = new BasicClientCookie(
			// "JSESSIONID", Global.getInstance().getSessionId());
			// basicClientCookie.setDomain("192.168.72.100");
			// cookieStore.addCookie(basicClientCookie);

			//判断是http请求还是https请求
			if(uri.getScheme().toLowerCase().equals("https")){
				response=httpsClient.execute(request);
			}
			else
				response = httpClient.execute(request);

		} catch (ClientProtocolException e) {
			throw new HttpException(e.getMessage(), e);
		} catch (IOException ioe) {
			throw new HttpException(ioe.getMessage(), ioe);
		}

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			// It will throw a Exception while status code is not 200
			handleResponseStatusCode(statusCode);
		} else {
			LogUtils.loge(LOG_TAG, "response is null");
		}

		return response;
	}

	/**
	 * 构造cookie字符串, 用于之后加到request
	 * 
	 * @param request
	 */
	public String buildCookiesStr4Request(Map<String, String> cookies) {

		StringBuilder sb = new StringBuilder(20);

		Set<Entry<String, String>> entries = cookies.entrySet();

		for (Entry<String, String> entry : entries) {

			String key = entry.getKey().toString();
			String val = entry.getValue().toString();

			sb.append(key);
			sb.append("=");
			sb.append(val);
			sb.append(";");

		}

		return sb.toString();
	}

	/**
	 * POST方式提交数据（包括一个文件和多个参数）
	 * 
	 * @param url
	 *            http url, 如:http://hyt.mamam100.com/user.action
	 * @param file
	 *            文件
	 * @param params
	 *            (key,value) 类似map的结构
	 * @return HttpResponse
	 * @throws HttpException
	 */
	// public HttpResponse post(String url, File file, List<BasicNameValuePair>
	// params)
	public HttpResponse post(String url, File file,
			List<BasicNameValuePair> params, Map<String, String> cookies)
			throws HttpException, SocketTimeoutException {

		LogUtils.logd(LOG_TAG, "post file " + " request to " + url);

		URI uri = createURI(url);

		HttpResponse response = null;

		HttpPost request = new HttpPost(uri);

		// Execute Request
		try {

			HttpEntity httpEntity = createMultipartEntity("myFile", file,
					params);
			request.setEntity(httpEntity);

			// Setup ConnectionParams, Request Headers
			setupHTTPConnectionParams(request);

			// 加cookies
			// 方法一，post里面设置头文件，在Header里加Cookie
			if (cookies != null && !cookies.isEmpty()) {
				// if (!cookies.isEmpty()) {
				String cookiesStr = buildCookiesStr4Request(cookies);
				// request.addHeader("cookie", cookiesStr);
				request.addHeader("Cookie", cookiesStr);
			}

			//判断是http请求还是https请求
			if(uri.getScheme().toLowerCase().equals("https")){
				response=httpsClient.execute(request);
			}
			else
				response = httpClient.execute(request);

		} catch (ClientProtocolException e) {
			throw new HttpException(e.getMessage(), e);
		} catch (IOException ioe) {
			throw new HttpException(ioe.getMessage(), ioe);
		}

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			// It will throw a Exception while status code is not 200
			handleResponseStatusCode(statusCode);
		} else {
			LogUtils.loge(LOG_TAG, "response is null");
		}

		return response;
	}
	
	/**
	 * HttpDelete 方式提交请求
	 * @param url http url, 如:http://hyt.mamam100.com/user.action
	 * @return HttpResponse
	 * @throws HttpException
	 */
	public HttpResponse delete(String url) throws HttpException {
		
		HttpResponse response = null;
		
		try {

//			LogUtils.loge(LOG_TAG, "url - "+url);
			URI uri = createURI(url);
//			LogUtils.loge(LOG_TAG, "uri - "+uri);
			HttpDelete delete = new HttpDelete(uri);
			
			// Setup ConnectionParams, Request Headers
			setupHTTPConnectionParams(delete);
			
			//判断是http请求还是https请求
			if(uri.getScheme().toLowerCase().equals("https")){
				response = httpsClient.execute(delete);
			} else {
				response = httpClient.execute(delete);
			}
			
		} catch (ClientProtocolException e) {
			throw new HttpException(e.getMessage(), e);
		} catch (IOException ioe) {
			throw new HttpException(ioe.getMessage(), ioe);
		}

		if (response != null) {
			int statusCode = response.getStatusLine().getStatusCode();
			// It will throw a Exception while status code is not 200
			handleResponseStatusCode(statusCode);
		} else {
			LogUtils.loge(LOG_TAG, "response is null");
		}
		
		return response;
	}
	
	

	/**
	 * 创建可带一个File的MultipartEntity
	 * 
	 * @param fileTag
	 *            文件tag
	 * @param file
	 *            文件
	 * @param postParams
	 *            其他POST参数
	 * @return 带文件和其他参数的Entity
	 * @throws UnsupportedEncodingException
	 */
	private MultipartEntity createMultipartEntity(String fileTag, File file,
			List<BasicNameValuePair> postParams)
			throws UnsupportedEncodingException {

		MultipartEntity entity = new MultipartEntity();
		// Don't try this. Server does not appear to support chunking.
		// entity.addPart("media", new InputStreamBody(imageStream, "media"));

		entity.addPart(fileTag, new FileBody(file));
		for (BasicNameValuePair param : postParams) {
			entity.addPart(param.getName(), new StringBody(param.getValue(), 
					Charset.forName("UTF-8")));
		}

		return entity;
	}

	/**
	 * CreateURI from URL string
	 * 
	 * @param url
	 * @return request URI
	 * @throws HttpException
	 *             Cause by URISyntaxException
	 */
	private URI createURI(String url) throws HttpException {

		URI uri;

		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			LogUtils.loge(LOG_TAG, e.getMessage());
			throw new HttpException("Invalid URL.");
		}

		return uri;
	}

	/**
	 * Setup HTTPConncetionParams
	 * 
	 * @param request
	 */
	private void setupHTTPConnectionParams(HttpUriRequest request) {

		HttpConnectionParams.setConnectionTimeout(request.getParams(),
				CONNECTION_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(request.getParams(),
				SOCKET_TIMEOUT_MS);
		httpClient.setHttpRequestRetryHandler(requestRetryHandler);
		httpsClient.setHttpRequestRetryHandler(requestRetryHandler);
		// method.addHeader("Accept-Encoding", "gzip, deflate");
		request.addHeader("Accept-Charset", "UTF-8,*;q=0.5");

	}

	/**
	 * 解析HTTP错误码
	 * 
	 * @param statusCode
	 * @return
	 */
	private static String getCause(int statusCode) {
		String cause = null;
		switch (statusCode) {
		case NOT_MODIFIED:
			break;
		case BAD_REQUEST:
			cause = "The request was invalid.  An accompanying error message will explain why. This is the status code will be returned during rate limiting.";
			break;
		case NOT_AUTHORIZED:
			cause = "Authentication credentials were missing or incorrect.";
			break;
		case FORBIDDEN:
			cause = "The request is understood, but it has been refused.  An accompanying error message will explain why.";
			break;
		case NOT_FOUND:
			cause = "The URI requested is invalid or the resource requested, such as a user, does not exists.";
			break;
		case NOT_ACCEPTABLE:
			cause = "Returned by the Search API when an invalid format is specified in the request.";
			break;
		case INTERNAL_SERVER_ERROR:
			cause = "internal server error";
			break;
		case BAD_GATEWAY:
			cause = "bad gateway error";
			break;
		case SERVICE_UNAVAILABLE:
			cause = "Service Unavailable: The servers are up, but overloaded with requests. Try again later. The search and trend methods use this to indicate when you are being rate limited.";
			break;
		default:
			cause = "";
		}
		return statusCode + ":" + cause;
	}

	/**
	 * Handle Status code
	 * 
	 * @param statusCode
	 *            响应的状态码
	 * @param res
	 *            服务器响应
	 * @throws HttpException
	 *             当响应码不为200时都会报出此异常:<br />
	 *             <li>HttpRequestException, 通常发生在请求的错误,如请求错误了 网址导致404等, 抛出此异常,
	 *             首先检查request log, 确认不是人为错误导致请求失败</li> <li>HttpAuthException,
	 *             通常发生在Auth失败, 检查用于验证登录的用户名/密码/KEY等</li> <li>
	 *             HttpRefusedException, 通常发生在服务器接受到请求, 但拒绝请求, 可是多种原因, 具体原因
	 *             服务器会返回拒绝理由, 调用HttpRefusedException#getError#getMessage查看</li>
	 *             <li>HttpServerException, 通常发生在服务器发生错误时, 检查服务器端是否在正常提供服务</li>
	 *             <li>HttpException, 其他未知错误.</li>
	 */
	private void handleResponseStatusCode(int statusCode) throws HttpException {

		String msg = getCause(statusCode) + "\n";
		LogUtils.loge(CoreHttpClient.class, msg);

		switch (statusCode) {
		// It's OK, do nothing
		case OK:
			break;

		// Mine mistake, Check the Log
		case NOT_MODIFIED:
		case BAD_REQUEST:
		case NOT_FOUND:
		case NOT_ACCEPTABLE:
			throw new HttpException(msg, statusCode);

			// Server will return a error message, use
			// HttpRefusedException#getError() to see.
		case FORBIDDEN:
			throw new HttpRefusedException(msg, statusCode);

			// Something wrong with server
		case INTERNAL_SERVER_ERROR:
		case BAD_GATEWAY:
		case SERVICE_UNAVAILABLE:
			throw new HttpServerException(msg, statusCode);

			// Others
		default:
			throw new HttpException(msg, statusCode);
		}

	}

	/**
	 * 异常自动恢复处理, 使用HttpRequestRetryHandler接口实现请求的异常恢复
	 */
	private static HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {

		// 自定义的恢复策略
		public boolean retryRequest(IOException exception, int executionCount,
				HttpContext context) {
			// 设置恢复策略，在发生异常时候将自动重试N次
			if (executionCount >= RETRIED_TIME) {
				// Do not retry if over max retry count
				return false;
			}
			if (exception instanceof NoHttpResponseException) {
				// Retry if the server dropped connection on us
				return true;
			}
			if (exception instanceof SSLHandshakeException) {
				// Do not retry on SSL handshake exception
				return false;
			}
			HttpRequest request = (HttpRequest) context
					.getAttribute(ExecutionContext.HTTP_REQUEST);
			boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
			if (!idempotent) {
				// Retry if the request is considered idempotent
				return true;
			}

			return false;
		}

	};

	/**
	 * 关闭连接管理器并释放资源
	 */
	public void shutdownHttpClient() {
		if (httpClient != null && httpClient.getConnectionManager() != null) {
			httpClient.getConnectionManager().shutdown();
			httpClient = null;
		}
		if (httpsClient != null && httpsClient.getConnectionManager() != null) {
			httpsClient.getConnectionManager().shutdown();
			httpsClient = null;
		}
	}

}

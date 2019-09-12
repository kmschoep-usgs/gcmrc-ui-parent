/*
 * 
 * Shamelessly stolen from GDP's OWSProxyServletX.
 * 
 * I killed the caching part, the blacklist, and the dynamic proxying.
 * 
 */
package gov.usgs.cida.proxy;

import gov.usgs.cida.gcmrc.util.PropertiesLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class AlternateProxyServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(AlternateProxyServlet.class);
	//protected static DynamicReadOnlyProperties props = null;
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Context context = propertiesLoader.getContextProps();
	// Connection pool setup
	private static final int CONNECTION_TTL = 15 * 60 * 1000;	   // 15 minutes, default is infinte
	private static final int CONNECTIONS_MAX_TOTAL = 256;
	private static final int CONNECTIONS_MAX_ROUTE = 32;
	// Connection timeouts
	private static final int CLIENT_SOCKET_TIMEOUT = 5 * 60 * 1000; // 5 minutes, default is infinite
	private static final int CLIENT_CONNECTION_TIMEOUT = 15 * 1000; // 15 seconds, default is infinte

	private static final String DEFAULT_FORWARD_URL = "http://localhost:8080/";

	private static final long serialVersionUID = 1L;
	protected ThreadSafeClientConnManager clientConnectionManager;
	protected HttpClient httpClient;
	private Set<String> ignoredClientRequestHeaderSet;
	private Set<String> ignoredServerResponseHeaderSet;
	private String forwardUrl;

	@Override
	public void init() throws ServletException {
		super.init();
		int maxTotalConns = getIntParam("max-total-connections", "max-total-connections-param", CONNECTIONS_MAX_TOTAL);
		int maxPerRouteConns = getIntParam("max-per-route-connections", "max-per-route-connections-param", CONNECTIONS_MAX_ROUTE);
		int connectionTtl = getIntParam("connection-ttl", "connection-ttl-param", CONNECTION_TTL);
		int clientSocketTimeout = getIntParam("client-socket-timeout", "client-socket-timeout-param", CLIENT_SOCKET_TIMEOUT);
		int clientConnectionTimeout = getIntParam("client-connection-timeout", "client-connection-timeout-param", CLIENT_CONNECTION_TIMEOUT);
		boolean ignoreClientAuthorization = getBoolParam("ignore-client-authorization", "ignore-client-authorization-param", Boolean.TRUE);
		boolean ignoreServerAuthorization = getBoolParam("ignore-server-authorization", "ignore-server-authorization-param", Boolean.TRUE);
		boolean handleRedirects = getBoolParam("handle-redirects", "handle-redirects-param", Boolean.TRUE);

		// Initialize connection manager, this is thread-safe.  if we use this
		// with any HttpClient instance it becomes thread-safe.
		clientConnectionManager = new ThreadSafeClientConnManager(SchemeRegistryFactory.createDefault(), connectionTtl, TimeUnit.MILLISECONDS);
		clientConnectionManager.setMaxTotal(maxTotalConns);
		clientConnectionManager.setDefaultMaxPerRoute(maxPerRouteConns);
		log.info("Created HTTP client connection manager: maximum connections total = " + clientConnectionManager.getMaxTotal() + ", maximum connections per route = " + clientConnectionManager.getDefaultMaxPerRoute());


		// Ignored headers relating to proxing requests
		ignoredClientRequestHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		ignoredClientRequestHeaderSet.add("host");		  // don't parameterize, need to swtich host from proxy to server
		ignoredClientRequestHeaderSet.add("connection");	// don't parameterize, let proxy to server call do it's own handling
		ignoredClientRequestHeaderSet.add("cookie");		// parameterize (cookies passthru?)
		if (ignoreClientAuthorization) {
			ignoredClientRequestHeaderSet.add("authorization"); // parameterize
		}
		ignoredClientRequestHeaderSet.add("content-length");// ignore header in request, this is set in client call.

		// Ignored headers relating to proxing reponses.
		ignoredServerResponseHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		ignoredServerResponseHeaderSet.add("transfer-encoding");// don't parameterize
		ignoredServerResponseHeaderSet.add("keep-alive");	   // don't parameterize
		ignoredServerResponseHeaderSet.add("set-cookie");	   // parameterize (cookies passthru?)
		if (ignoreServerAuthorization) {
			ignoredServerResponseHeaderSet.add("authorization");	// parameterize
		}
		Set<String> customIgnoredServerResponseHeaderSet = this.getCustomIgnoredServerResponseHeaders();
		ignoredServerResponseHeaderSet.addAll(customIgnoredServerResponseHeaderSet);

		this.forwardUrl = getStringParam("forward-url", "forward-url-param", DEFAULT_FORWARD_URL);

		if (StringUtils.equals(this.forwardUrl, DEFAULT_FORWARD_URL)) {
			log.warn("forward-url was not specified in web.xml. Defaulting to http://localhost:8080/");
		}

		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, clientSocketTimeout);
		HttpConnectionParams.setConnectionTimeout(httpParams, clientConnectionTimeout);
		httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, handleRedirects);

		httpClient = new DefaultHttpClient(clientConnectionManager, httpParams);
	}

	@Override
	public void destroy() {
		super.destroy();
		clientConnectionManager.shutdown();
	}

	protected HttpClient getHttpClient(HttpServletRequest clientRequest) {
		// this could be extended to return client specific HttpClients in
		// this future.  An example use case is if authorization were enabled
		// and we wanted to provide each client endpoint with it's own 
		// CachedHttpClient with a private cache to store authorized response 
		// content with its own cache storage.  In the absense of this
		// we can just return a singleton client.
		return httpClient;
	}

	@Override
	protected void service(HttpServletRequest clientRequest, HttpServletResponse clientResponse) throws ServletException, IOException {
		try {

			proxyRequest(clientRequest, clientResponse);

		} catch (Exception e) {
			// Important!  With Apache Tomcat 6 container uncaught exceptions will result in
			// container returning HTTP status code 200 (OK) w/ empty response body.
			proxyError(clientRequest, clientResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			// log a little more information to help with debugging as this is a bas state...
			StringBuilder logMessageBuilder = new StringBuilder();
			logMessageBuilder.append("Uncaught exception handling proxy request from ").
				append(getClientRequestURIAsString(clientRequest)).append(" to ").
				append(getServerRequestURIAsString(clientRequest));
			log.error(logMessageBuilder.toString(), e);
		} finally {
			clientResponse.flushBuffer();
		}
	}

	protected void proxyRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse) {
		try {
			HttpUriRequest serverRequest = generateServerRequest(clientRequest);
			handleServerRequest(clientRequest, clientResponse, serverRequest);
		} catch (ProxyException e) {
			proxyError(clientRequest, clientResponse, e);
		}
	}

	protected void proxyError(HttpServletRequest clientRequest, HttpServletResponse clientResponse, ProxyException exception) {
		StringBuilder errorBuilder = new StringBuilder(exception.getMessage());
		Throwable cause = exception.getRootCause();
		if (cause != null) {
			errorBuilder.append(", exception message: ").append(cause.getMessage());
		}
		proxyError(clientRequest, clientResponse, HttpServletResponse.SC_BAD_REQUEST, errorBuilder.toString());
	}

	protected void proxyError(HttpServletRequest clientRequest, HttpServletResponse clientResponse, int errorCode, String errorMessage) {
		PrintWriter writer = null;
		try {
			log.error("Error proxying request from " + getClientRequestURIAsString(clientRequest) + " to " + getServerRequestURIAsString(clientRequest) + ". " + errorMessage);
			clientResponse.setStatus(errorCode);
			clientResponse.setCharacterEncoding("UTF-8");
			writer = clientResponse.getWriter();
			writer.println(errorMessage);
		} catch (IOException e) {
			log.error("Error writing error message to client: " + errorCode + " " + errorMessage + ", exception message: " + e.getMessage());
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	protected HttpUriRequest generateServerRequest(HttpServletRequest clientRequest) throws ProxyException {
		HttpUriRequest serverRequest = null;
		try {

			// 1) Generate Server URI
			String serverRequestURIAsString = getServerRequestURIAsString(clientRequest);
			// instantiating to URL then calling toURI gives us some error
			// checking as URI(String) appears too forgiving.
			URI serverRequestURI = (new URL(serverRequestURIAsString)).toURI();

			// 2 ) Create request base on client request method
			String clientRequestMethod = clientRequest.getMethod();
			if ("HEAD".equals(clientRequestMethod)) {
				serverRequest = new HttpHead(serverRequestURI);
			} else if ("GET".equals(clientRequestMethod)) {
				serverRequest = new HttpGet(serverRequestURI);
			} else if ("POST".equals(clientRequestMethod)) {
				serverRequest = new HttpPost(serverRequestURI);
			} else if ("PUT".equals(clientRequestMethod)) {
				serverRequest = new HttpPut(serverRequestURI);
			} else if ("DELETE".equals(clientRequestMethod)) {
				serverRequest = new HttpDelete(serverRequestURI);
			} else if ("TRACE".equals(clientRequestMethod)) {
				serverRequest = new HttpTrace(serverRequestURI);
			} else if ("OPTIONS".equals(clientRequestMethod)) {
				serverRequest = new HttpOptions(serverRequestURI);
			} else {
				throw new ProxyException("Unsupported request method, " + clientRequestMethod);
			}

			// 3) Map client request headers to server request
			generateServerRequestHeaders(clientRequest, serverRequest);

			// 4) Copy client request body to server request
			int contentLength = clientRequest.getContentLength();
			if (contentLength > 0) {
				if (serverRequest instanceof HttpEntityEnclosingRequest) {
					try {
						// !!! Are you here to edit this to enable request body content rewrite?
						//	 You may want to remove or edit the "Content-Length" header !!!
						InputStreamEntity serverRequestEntity = new InputStreamEntity(
								clientRequest.getInputStream(),
								contentLength);
						serverRequestEntity.setContentType(clientRequest.getContentType());
						((HttpEntityEnclosingRequest) serverRequest).setEntity(serverRequestEntity);
					} catch (IOException e) {
						throw new ProxyException("Error reading client request body", e);
					}
				} else {
					throw new ProxyException("Content in request body unsupported for client request method " + serverRequest.getMethod());
				}
			}

		} catch (MalformedURLException|URISyntaxException e) {
			throw new ProxyException("Syntax error parsing server URL", e);
		}

		return serverRequest;
	}

	protected void handleServerRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpUriRequest serverRequest) throws ProxyException {
		HttpClient serverClient = getHttpClient(clientRequest);
		try {
			HttpContext localContext = new BasicHttpContext();
			HttpResponse methodReponse = serverClient.execute(serverRequest, localContext);
			handleServerResponse(clientRequest, clientResponse, methodReponse);
		} catch (ClientProtocolException e) {
			throw new ProxyException("Client protocol error", e);
		} catch (IOException e) {
			throw new ProxyException("I/O error on server request", e);
		}

	}

	protected String getClientRequestURIAsString(HttpServletRequest clientRequest) {
		return clientRequest.getRequestURL().toString();
	}

	/**
	* TODO, this returns the wrong stuff when there isn't an ending slash.
	*
	* @param clientrequest
	* @return
	*/
	protected String getServerRequestURIAsString(HttpServletRequest clientrequest) {
		String proxyPath = new StringBuilder(clientrequest.getContextPath()).
				append(clientrequest.getServletPath()).
				append('/').
				toString();

		StringBuilder requestBuffer = new StringBuilder(clientrequest.getRequestURI());
		// request query string is *not* URL decoded
		String requestQueryString = clientrequest.getQueryString();
		if (requestQueryString != null) {
			requestBuffer.append('?').append(requestQueryString);
		}

		StringBuilder result = new StringBuilder();

		String fwdUrl = this.forwardUrl;
		String restOfPath = "";

		if (proxyPath.length() < requestBuffer.length()) {
			restOfPath = requestBuffer.substring(proxyPath.length());
		}

		result.append(fwdUrl);
		boolean endsWith = fwdUrl.endsWith("/");
		boolean startsWith = restOfPath.startsWith("/");
		if (endsWith && startsWith) {
			result.delete(result.length() - 1, result.length());
		} else if (!endsWith && !startsWith) {
			result.append("/");
		}
	result.append(restOfPath);

		return result.toString();
	}
	
	protected void generateServerRequestHeaders(HttpServletRequest clientRequest, HttpUriRequest serverRequest) {

		Enumeration<String> headerNameEnumeration = clientRequest.getHeaderNames();
		while (headerNameEnumeration.hasMoreElements()) {
			String requestHeaderName = headerNameEnumeration.nextElement();
			Enumeration<String> headerValueEnumeration = clientRequest.getHeaders(requestHeaderName);
			while (headerValueEnumeration.hasMoreElements()) {
				String requestHeaderValue = headerValueEnumeration.nextElement();
				if (!ignoredClientRequestHeaderSet.contains(requestHeaderName)) {
					serverRequest.addHeader(requestHeaderName, requestHeaderValue);
					log.debug("Mapped client request header \"" + requestHeaderName + ": " + requestHeaderValue + "\"");
				} else {
					log.debug("Ignored client request header \"" + requestHeaderName + ": " + requestHeaderValue + "\"");
				}
			}

		}

		URI serverURI = serverRequest.getURI();
		StringBuilder serverHostBuilder = new StringBuilder(serverURI.getHost());
		if (serverURI.getPort() > -1) {
			serverHostBuilder.append(':').append(serverURI.getPort());
		}
		String requestHost = serverHostBuilder.toString();
		serverRequest.addHeader("Host", serverHostBuilder.toString());
		log.debug("Added server request header \"Host: " + requestHost + "\"");
	}

	protected void generateClientResponseHeaders(HttpServletResponse clientResponse, HttpResponse serverResponse) {
		Header[] proxyResponseHeaders = serverResponse.getAllHeaders();
		for (Header header : proxyResponseHeaders) {
			String responseHeaderName = header.getName();
			String responseHeaderValue = header.getValue();
			if (!ignoredServerResponseHeaderSet.contains(responseHeaderName)) {
				clientResponse.addHeader(responseHeaderName, responseHeaderValue);
				log.debug("Mapped server response header \"" + responseHeaderName + ": " + responseHeaderValue + "\"");
			} else {
				log.debug("Ignored server response header \"" + responseHeaderName + ": " + responseHeaderValue + "\"");
			}
		}
	}

	protected void handleServerResponse(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse serverResponse) throws ProxyException {

		String clientRequestURLAsString = getClientRequestURIAsString(clientRequest);
		String serverRequestURLAsString = getServerRequestURIAsString(clientRequest);

		// 1) Map server response status to client response
		// NOTE: There's no clear way to map status message, HttpServletResponse.sendError(int, String)
		// will display some custom html (we don't want that here).  HttpServletResponse.setStatus(int, String)
		// is deprecated and i'm not certain there is (will be) an functional implementation behind it.
		StatusLine serverStatusLine = serverResponse.getStatusLine();
		int statusCode = serverStatusLine.getStatusCode();
		clientResponse.setStatus(statusCode);
		log.debug("Mapped server status code " + statusCode);

		// 2) Map server response headers to client response
		generateClientResponseHeaders(clientResponse, serverResponse);


		// 3) Copy server response body to client response
		HttpEntity methodEntity = serverResponse.getEntity();
		if (methodEntity != null) {

			InputStream is = null;
			OutputStream os = null;

			long responseBytes = 0;

			try {

				// !!! Are you here to edit this to enable response body content rewrite?
				//	 You may want to remove or edit the "Content-Length" header !!!
				try {
					is = methodEntity.getContent();
				} catch (IOException e) {
					throw new ProxyException("Error obtaining input stream for server response", e);
				}

				try {
					os = clientResponse.getOutputStream();
				} catch (IOException e) {
					throw new ProxyException("Error obtaining output stream for client response", e);
				}

				try {
					responseBytes = IOUtils.copyLarge(is, os);
				} catch (IOException e) {
					throw new ProxyException("Error copying server response to client", e);
				}

			} finally {
				log.debug("Copied " + responseBytes + " bytes from server response for proxy from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
				try {
					// This is important to guarantee connection release back into
					// connection pool for future reuse!
					EntityUtils.consume(methodEntity);
				} catch (IOException e) {
					log.error("Error consuming remaining bytes in server response entity for proxy reponse from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
				}
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		} else {
			log.warn("Server response was empty for proxy response from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
		}
	}
	
	private Object getParamFromInitOrJNDI(String initParam, String initParamJNDIName, Object defaultValue) {
		Object result = defaultValue;
		String jndiParamName = this.getInitParameter(initParamJNDIName);
		String paramValue = StringUtils.isBlank(jndiParamName) ? this.getInitParameter(initParam)
				: propertiesLoader.getProp(context, jndiParamName, this.getInitParameter(initParam));
		if (null != paramValue) {
			result = paramValue;
		}
		return result;
	}
	
	protected int getIntParam(String initParam, String initParamJNDIName, Integer defaultValue) throws ServletException{
		int intValue;
		Object param = getParamFromInitOrJNDI(initParam, initParamJNDIName, defaultValue);
		try {
			if (param instanceof String) {
				intValue = Integer.parseInt((String)param);
			} else if (param instanceof Integer) {
				intValue = (Integer)param;
			} else {
				throw new IllegalArgumentException("Integer params only please");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		return intValue;
	}
	
	protected long getLongParam(String initParam, String initParamJNDIName, Long defaultValue) throws ServletException{
		long longValue;
		Object param = getParamFromInitOrJNDI(initParam, initParamJNDIName, defaultValue);
		try {
			if (param instanceof String) {
				longValue = Long.parseLong((String)param);
			} else if (param instanceof Long) {
				longValue = (Long)param;
			} else {
				throw new IllegalArgumentException("Long params only please");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		return longValue;
	}
	
	protected boolean getBoolParam(String initParam, String initParamJNDIName, Boolean defaultValue) throws ServletException{
		boolean boolValue;
		Object param = getParamFromInitOrJNDI(initParam, initParamJNDIName, defaultValue);
		try {
			if (param instanceof String) {
				boolValue = Boolean.parseBoolean((String)param);
			} else if (param instanceof Boolean) {
				boolValue = (Boolean)param;
			} else {
				throw new IllegalArgumentException("Boolean params only please");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		return boolValue;
	}
	
	protected String getStringParam(String initParam, String initParamJNDIName, String defaultValue) throws ServletException{
		String strValue;
		Object param = getParamFromInitOrJNDI(initParam, initParamJNDIName, defaultValue);
		try {
			if (param instanceof String) {
				strValue = (String)param;
			} else {
				throw new IllegalArgumentException("String params only please");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		return strValue;
	}

	private Set<String> getCustomIgnoredServerResponseHeaders() throws ServletException {
			Set<String> ignoredHeaders = new HashSet<String>();
			String unsplitHeaders = getStringParam("ignored-server-response-headers", "ignored-server-response-headers-param", "").trim();
			if(unsplitHeaders.length() > 0){
					String[] splitCustomHeaders = unsplitHeaders.split(",");
					for(String customHeader : splitCustomHeaders){
							customHeader = customHeader.trim();
							ignoredHeaders.add(customHeader);
					}
			}
			return ignoredHeaders;
		}

	public static class ProxyException extends Exception {

		public ProxyException(String message) {
			super(message);
		}

		public ProxyException(String message, Throwable cause) {
			super(message, cause);
		}

		public Throwable getRootCause() {
			return getRootCause(getCause());
		}

		private Throwable getRootCause(Throwable t) {
			return t.getCause() == null ? t : getRootCause(t.getCause());
		}
	}
}

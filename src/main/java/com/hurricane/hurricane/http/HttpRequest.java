package com.hurricane.hurricane.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import static com.hurricane.hurricane.common.Constant.*;


/**
 * @author larrytaowang
 */
public class HttpRequest {
  private final static Logger logger = Logger.getLogger(HttpRequest.class);

  /**
   * The Http connection this request belongs to
   */
  private HttpConnection httpConnection;

  /**
   * Method of the Http request, parsed from start line.
   */
  private HttpMethod method;

  /**
   * Uri of the Http request, parsed from start line.
   */
  private String uri;

  /**
   * Query Key Value pairs in the URI
   */
  private Map<String, String> queryArgs;

  /**
   * The path component of this request
   */
  private String path;

  /**
   * Version of Http request, parsed from start line. Version higher than 1.1 is not supported.
   */
  private String version;

  /**
   * Headers of this request
   */
  private HttpHeaders httpHeaders;

  /**
   * Body of the Http request
   */
  private HttpBody httpBody;

  public HttpRequest(HttpConnection connection, HttpMethod method, String uri, String version,
      HttpHeaders httpHeaders) {
    this.httpConnection = connection;
    this.method = method;
    this.uri = uri;
    parseUri(uri);

    this.version = version;
    this.httpHeaders = httpHeaders;
  }

  /**
   * Parse the uri
   *
   * @param uri uri to parse
   */
  private void parseUri(String uri) {
    // Parse the path component
    var uriDelimiter = '?';
    if (uri.indexOf(uriDelimiter) != -1) {
      this.path = uri.substring(0, uri.indexOf(uriDelimiter));
    } else {
      this.path = uri;
    }

    // Parse the query arguments
    List<NameValuePair> params;
    try {
      params = URLEncodedUtils.parse(new URI(uri), StandardCharsets.UTF_8);
    } catch (URISyntaxException e) {
      logger.warn("Malformed URI = " + uri);
      return;
    }

    this.queryArgs = new HashMap<>();
    for (var param : params) {
      this.queryArgs.put(param.getName(), param.getValue());
    }
  }

  /**
   * Return true if this request supports HTTP/1.1 semantics
   *
   * @return if this request supports HTTP/1.1
   */
  public boolean supportHttpOneDotOne() {
    return version.equals(HTTP_VERSION_1_1);
  }

  /**
   * Check the headers to see if the server should disconnect after the request is finished.
   *
   * @return If the request should be disconnected after finished.
   */
  public boolean disconnectWhenFinish() {
    var headerConnectionValue = httpHeaders.getValues(HTTP_HEADER_KEY_CONNECTION);

    if (supportHttpOneDotOne()) {
      // The header value of "Connection" can be either "keep-alive" or "close". Disconnect if it is "close".
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_CONNECTION_CLOSE);
    } else if (httpHeaders.contains(HTTP_HEADER_KEY_CONTENT_LENGTH)) {
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_KEEP_ALIVE);
    } else if (method.equals(HttpMethod.HEAD) || method.equals(HttpMethod.GET)) {
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_KEEP_ALIVE);
    } else {
      return true;
    }
  }

  /**
   * Parse the string content and construct a Http request with headers
   *
   * @param httpRequestLines String content of the Http request
   * @param httpConnection   The Http connection the new request belongs to
   * @return A Http request object generated from the given content.
   */
  public static HttpRequest parseHttpRequestHeaders(String httpRequestLines, HttpConnection httpConnection)
      throws HttpException {
    // Parse First Line of Http Request
    var delimiterIndex = httpRequestLines.indexOf(HTTP_HEADER_KEY_VALUE_DELIMITER);
    if (delimiterIndex == -1) {
      throw new IllegalArgumentException("Malformed Http Header, cannot find delimiter \"\r\n\" for start line");
    }

    var startLine = httpRequestLines.substring(0, delimiterIndex);
    var fields = startLine.split(" ");
    var methodOptional = HttpMethod.fromString(fields[0].strip());
    if (methodOptional.isEmpty()) {
      throw new HttpException(HttpStatus.BAD_REQUEST);
    }
    var method = methodOptional.get();

    var uri = fields[1];
    var version = fields[2];

    if (!version.startsWith(HTTP_VERSION_PREFIX)) {
      throw new IllegalArgumentException("Malformed HTTP version in Request-Line. version = " + version);
    }

    // Parse remaining lines as Http Headers
    var httpHeadersLine = httpRequestLines.substring(delimiterIndex + HTTP_HEADER_KEY_VALUE_DELIMITER.length());
    var httpHeaders = HttpHeaders.parse(httpHeadersLine);

    // Create Http Request
    logger.info("Finish parsing HTTP Request headers. METHOD = " + method + ", URI = " + uri + ", VERSION = " + version
        + ", HEADERS = " + httpHeaders);
    return new HttpRequest(httpConnection, method, uri, version, httpHeaders);
  }

  /**
   * Parse the HTTP body with the given data. Right now only "application/x-www-form-urlencoded" is supported.
   *
   * @param data bytes of the HTTP body
   */
  public void parseBody(byte[] data) {
    httpBody = new HttpBody(data);
    var contentType = getHttpHeaders().getValues(HTTP_HEADER_KEY_CONTENT_TYPE);

    if (getMethod().equals(HttpMethod.POST)) {
      if (contentType.startsWith(HTTP_APPLICATION_X_WWW_FORM_URLENCODED)) {
        httpBody.parseFormUrlEncodedBody();
      }
    }
  }

  /**
   * Finish the request.
   */
  public void finish() {
    httpConnection.finish();
  }

  /**
   * Write data to the browser.
   *
   * @param chunk data to write
   */
  public void write(byte[] chunk) {
    httpConnection.write(chunk);
  }

  /**
   * Return summary of this Http request
   *
   * @return summary of this Http request
   */
  public String summary() {
    return method.toString() + " " + uri;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public String toString() {
    return "HttpRequest{" + "method='" + method + '\'' + ", uri='" + uri + '\'' + ", version='" + version + '\''
        + ", httpHeaders=" + httpHeaders + ", body=" + httpBody + '}';
  }

  public String getUri() {
    return uri;
  }

  public String getVersion() {
    return version;
  }

  public HttpBody getHttpBody() {
    return httpBody;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getQueryArgs() {
    return queryArgs;
  }
}

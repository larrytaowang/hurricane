package com.hurricane.hurricane.http;

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
  private String method;

  /**
   * Uri of the Http request, parsed from start line.
   */
  private String uri;

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

  public HttpRequest(HttpConnection connection, String method, String uri, String version, HttpHeaders httpHeaders) {
    this.httpConnection = connection;
    this.method = method;
    this.uri = uri;
    this.version = version;
    this.httpHeaders = httpHeaders;
  }

  /**
   * Return true if this request supports HTTP/1.1 semantics
   * @return if this request supports HTTP/1.1
   */
  public boolean supportHttpOneDotOne() {
    return version.equals(HTTP_VERSION_1_1);
  }

  /**
   * Check the headers to see if the server should disconnect after the request is finished.
   * @return If the request should be disconnected after finished.
   */
  public boolean disconnectWhenFinish() {
    var headerConnectionValue = httpHeaders.getValues(HTTP_HEADER_KEY_CONNECTION);

    if (supportHttpOneDotOne()) {
      // The header value of "Connection" can be either "keep-alive" or "close". Disconnect if it is "close".
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_CONNECTION_CLOSE);
    } else if (httpHeaders.contains(HTTP_HEADER_KEY_CONTENT_LENGTH)) {
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_KEEP_ALIVE);
    } else if (method.equals(HTTP_METHOD_HEAD) || method.equals(HTTP_METHOD_GET)) {
      return headerConnectionValue.equals(HTTP_HEADER_CONNECTION_VALUE_KEEP_ALIVE);
    } else {
      return true;
    }
  }

  /**
   * Parse the string content and construct a Http request with headers
   * @param httpRequestLines String content of the Http request
   * @param httpConnection The Http connection the new request belongs to
   * @return A Http request object generated from the given content.
   */
  public static HttpRequest parseHttpRequestHeaders(String httpRequestLines, HttpConnection httpConnection) {
    // Parse First Line of Http Request
    var delimiterIndex = httpRequestLines.indexOf(HTTP_HEADER_KEY_VALUE_DELIMITER);
    if (delimiterIndex == -1) {
      throw new IllegalArgumentException("Malformed Http Header, cannot find delimiter \"\r\n\" for start line");
    }

    var startLine = httpRequestLines.substring(0, delimiterIndex);
    var fields = startLine.split(" ");
    var method = fields[0];
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
   * @param data bytes of the HTTP body
   */
  public void parseBody(byte[] data) {
    httpBody = new HttpBody(data);
    var contentType = getHttpHeaders().getValues(HTTP_HEADER_KEY_CONTENT_TYPE);

    if (getMethod().equals(HTTP_METHOD_POST)) {
      if (contentType.startsWith(HTTP_APPLICATION_X_WWW_FORM_URLENCODED)) {
        httpBody.parseFormUrlEncodedBody();
      }
    }
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public String getMethod() {
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
}

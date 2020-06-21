package com.hurricane.hurricane.common;

/**
 * @author larrytaowang
 */
public class Constant {
  public static final String HTTP_VERSION_PREFIX = "HTTP/";
  public static final String HTTP_VERSION_1_1 = "HTTP/1.1";

  public static final String HTTP_HEADER_KEY_CONNECTION = "Connection";
  public static final String HTTP_HEADER_CONNECTION_VALUE_KEEP_ALIVE = "Keep-Alive";
  public static final String HTTP_HEADER_CONNECTION_VALUE_CONNECTION_CLOSE = "close";

  public static final String HTTP_HEADER_KEY_CONTENT_LENGTH = "Content-Length";
  public static final String HTTP_HEADER_KEY_CONTENT_TYPE = "Content-Type";

  public static final String HTTP_HEADER_KEY_EXPECT = "Expect";
  public static final String HTTP_HEADER_EXPECT_VALUE_100_CONTINUE = "100-continue";

  public static final String HTTP_HEADER_DELIMITER = "\r\n\r\n";
  public static final String HTTP_HEADER_KEY_VALUE_DELIMITER = "\r\n";

  public static final String HTTP_METHOD_HEAD = "HEAD";
  public static final String HTTP_METHOD_GET = "GET";
  public static final String HTTP_METHOD_POST = "POST";
  public static final String HTTP_METHOD_DELETE = "DELETE";
  public static final String HTTP_METHOD_PUT = "PUT";

  public static final String HTTP_APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

  public static final String HTTP_100_CONTINUE_RESPONSE = "HTTP/1.1 100 (Continue)\r\n\r\n";
}

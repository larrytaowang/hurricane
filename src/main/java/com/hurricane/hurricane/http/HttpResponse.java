package com.hurricane.hurricane.http;

import java.nio.charset.StandardCharsets;

import static com.hurricane.hurricane.common.Constant.*;


/**
 * @author larrytaowang
 * <p>
 * Reponse of a Http Request
 */
public class HttpResponse {
  /**
   * Headers of this response
   */
  private HttpHeaders headers;

  /**
   * Body of this Http response.
   */
  private StringBuilder body;

  /**
   * Status of this Http response. By default it is 200 OK.
   */
  private HttpStatus status;

  /**
   * If the header of this Http response has been writen to browser. RequestHandler should check this flag to avoid
   * sending the Http header multiple times.
   */
  private boolean isHeaderWritten;

  public HttpResponse() {
    this(new HttpHeaders());
  }

  public HttpResponse(HttpHeaders headers) {
    this.isHeaderWritten = false;
    this.headers = headers;
    this.body = new StringBuilder();
    this.status = HttpStatus.OK;
  }

  /**
   * Append chunk to the Http response
   *
   * @param chunk chunk added to the response
   */
  public void append(String chunk) {
    body.append(chunk);
  }

  /**
   * This should only be called when we want to write the response body. The bytes of the headers will be returned and
   * isHeaderWritten will be set true.
   *
   * @param httpVersion Http version of this request
   * @return bytes of the headers of this Http response
   */
  public byte[] getHeadersBytes(String httpVersion) {
    // Append header string
    var whiteSpace = ' ';
    var result =
        httpVersion + whiteSpace + status.getCode() + whiteSpace + status.name() + HTTP_HEADER_KEY_VALUE_DELIMITER;

    // Append headers
    result += headers.getContent() + HTTP_HEADER_KEY_VALUE_DELIMITER;
    isHeaderWritten = true;

    return result.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Get bytes of HTTP response encoded in UTF8. This should only be called when are about to send the body. So, clean
   * the body when the bytes are sent to the caller.
   *
   * @return bytes of HTTP response
   */
  public byte[] getBodyBytes() {
    var bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
    body.setLength(0);
    return bodyBytes;
  }

  public boolean isHeaderWritten() {
    return isHeaderWritten;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  public HttpStatus getStatus() {
    return status;
  }
}

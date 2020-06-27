package com.hurricane.hurricane.http;

/**
 * @author larrytaowang
 */
public enum HttpStatus {
  /**
   * The server could not understand the request, probably due to a syntax error.
   */
  BAD_REQUEST(400),

  /**
   * The method used by the client is not supported by this URL. The methods that are supported must be listed in the
   * response's Allow header.
   */
  METHOD_NOT_ALLOWED(405);

  private int code;

  HttpStatus(int code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return "HttpStatus{" + "code=" + code + ", message='" + name().replace('_', ' ') + '\'' + '}';
  }
}

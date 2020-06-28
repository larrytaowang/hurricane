package com.hurricane.hurricane.http;

/**
 * @author larrytaowang
 */
public enum HttpStatus {
  /**
   * The client's request was successful and the server's response contains the requested data. This is the default
   * status code.
   */
  OK(200),

  /**
   * The server could not understand the request, probably due to a syntax error.
   */
  BAD_REQUEST(400),

  /**
   * The method used by the client is not supported by this URL. The methods that are supported must be listed in the
   * response's Allow header.
   */
  METHOD_NOT_ALLOWED(405),

  /**
   * An unexpected error occurred inside the server that prevented it from fulfilling the request.
   */
  INTERNAL_SERVER_ERROR(500);

  private int code;

  HttpStatus(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  @Override
  public String toString() {
    return "HttpStatus{" + "code=" + code + ", message='" + name().replace('_', ' ') + '\'' + '}';
  }
}

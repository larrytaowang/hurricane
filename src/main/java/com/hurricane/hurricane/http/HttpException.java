package com.hurricane.hurricane.http;

/**
 * @author larrytaowang
 */
public class HttpException extends RuntimeException {
  private static long serialVersionUID = 1L;

  private HttpStatus status;

  public HttpException(HttpStatus status) {
    super(status.toString());
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}

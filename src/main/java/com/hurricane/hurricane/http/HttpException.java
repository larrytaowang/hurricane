package com.hurricane.hurricane.http;

/**
 * @author larrytaowang
 */
public class HttpException extends RuntimeException {
  private static long serialVersionUID = 1L;

  public HttpException(HttpStatus status) {
    super(status.toString());
  }
}

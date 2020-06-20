package com.hurricane.hurricane.http;

/**
 * @author larrytaowang
 * This callback will be run after HTTP server finishes parsing HTTP request.
 */
public interface HttpRequestCallback {
  /**
   * Run the callback after HTTP server finishes parsing HTTP request.
   * @param connection TCP connection that this callback hosts
   * @param request HTTP request of this callback
   */
  void run(HttpConnection connection, HttpRequest request);
}

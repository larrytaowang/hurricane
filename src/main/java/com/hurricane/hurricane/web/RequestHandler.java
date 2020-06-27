package com.hurricane.hurricane.web;

import com.hurricane.hurricane.http.HttpConnection;
import com.hurricane.hurricane.http.HttpMethod;
import com.hurricane.hurricane.http.HttpRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author larrytaowang
 */
public abstract class RequestHandler{

  /**
   * Check if a method is supported in this handler
   * @param method a HTTP handler
   * @return if a method is supported in this handler
   */
  protected boolean isMethodSupported(HttpMethod method) {
    return getSupportedMethods().contains(method);
  }

  /**
   * If a subclass wants to support more methods than the standard GET/HEAD/POST, it should override this method
   * @return All the HTTP methods that are supported in this handler
   */
  protected Set<HttpMethod> getSupportedMethods() {
    var methods = Arrays.asList(HttpMethod.values());
    return new HashSet<>(methods);
  }

  /**
   * Run the callback after HTTP server finishes parsing HTTP request.
   * @param connection TCP connection that this callback hosts
   * @param request HTTP request of this callback
   */
  public abstract void run(HttpConnection connection, HttpRequest request);
}

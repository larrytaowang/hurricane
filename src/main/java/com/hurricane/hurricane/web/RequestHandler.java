package com.hurricane.hurricane.web;

import com.hurricane.hurricane.http.HttpRequestCallback;
import java.util.Arrays;
import java.util.List;

import static com.hurricane.hurricane.common.Constant.*;


/**
 * @author larrytaowang
 */
public abstract class RequestHandler implements HttpRequestCallback {

  /**
   * Check if a method is supported in this handler
   * @param method a HTTP handler
   * @return if a method is supported in this handler
   */
  protected boolean isMethodSupported(String method) {
    return getSupportedMethods().contains(method);
  }

  /**
   * If a subclass wants to support more methods than the standard GET/HEAD/POST, it should override this method
   * @return All the HTTP methods that are supported in this handler
   */
  protected List<String> getSupportedMethods() {
    return Arrays.asList(HTTP_METHOD_GET, HTTP_METHOD_HEAD, HTTP_METHOD_POST, HTTP_METHOD_DELETE, HTTP_METHOD_PUT);
  }
}

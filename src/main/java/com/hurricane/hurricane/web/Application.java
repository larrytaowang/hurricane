package com.hurricane.hurricane.web;

import com.hurricane.hurricane.http.HttpConnection;
import com.hurricane.hurricane.http.HttpRequest;
import java.util.LinkedList;
import java.util.List;


/**
 * @author larrytaowang
 * <p>
 * A collection of request handlers that make up a web application.
 */
public class Application {

  /**
   * a list of (url, handler) pair
   */
  private LinkedList<UrlSpec> urlSpecs;

  /**
   * The constructor for this class takes in a list of URLSpec objects or (regexp, request_class) tuples. When we
   * receive requests, we iterate over the list in order and instantiate an instance of the first request class whose
   * regexp matches the request path.
   *
   * @param urlSpecs collection of request handlers that make up a web application
   */
  public Application(List<UrlSpec> urlSpecs) {
    this.urlSpecs = new LinkedList<>();
    this.urlSpecs.addAll(urlSpecs);
  }

  /**
   * Given a HttpRequest, find the associated handler to process it.
   *
   * @param request an Http Request
   * @return a Handler that can process the Http request
   */
  private RequestHandler findMatchHandler(HttpRequest request) {
    var path = request.getPath();
    for (var urlSpec : this.urlSpecs) {
      var match = urlSpec.getPattern().matcher(path).matches();
      if (match) {
        return urlSpec.getHandler();
      }
    }

    return null;
  }

  /**
   * Process a Http request
   *
   * @param connection  Http connection of the request
   * @param httpRequest the http request that will be handled
   */
  public void run(HttpConnection connection, HttpRequest httpRequest) {
    var handler = findMatchHandler(httpRequest);
    if (handler != null) {
      handler.run(connection, httpRequest);
    }
  }
}

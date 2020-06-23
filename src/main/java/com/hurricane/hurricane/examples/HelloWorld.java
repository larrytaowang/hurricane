package com.hurricane.hurricane.examples;

import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.http.HttpException;
import com.hurricane.hurricane.http.HttpRequest;
import com.hurricane.hurricane.http.HttpServer;
import com.hurricane.hurricane.web.Application;
import com.hurricane.hurricane.web.RequestHandler;
import com.hurricane.hurricane.web.UrlSpec;
import java.io.IOException;
import java.util.Collections;


/**
 * @author larrytaowang
 * <p>
 * Return response "Hello World!" for each url
 */
public class HelloWorld {
  public static void main(String[] args) throws IOException {
    RequestHandler requestHandler = new RequestHandler() {
      @Override
      protected void handleGetMethod(HttpRequest request) throws HttpException {
        write("Hello World!");
      }
    };

    var application = new Application(Collections.singletonList(new UrlSpec(".*", requestHandler)));

    HttpServer httpServer = HttpServer.getInstance();
    httpServer.setApplication(application);
    httpServer.listen(8888);

    EventLoop.getInstance().start();
  }
}

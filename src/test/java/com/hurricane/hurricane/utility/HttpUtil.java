package com.hurricane.hurricane.utility;

import com.hurricane.hurricane.http.HttpServer;
import com.hurricane.hurricane.web.RequestHandler;
import java.io.IOException;


public class HttpUtil {
  /**
   * Binds the HTTP server to an ephemeral port and listens to new connections.
   * @param callback callback that will be run when HTTP server finishes parsing HTTP request
   * @throws IOException Some IO errors in bind and listen operations.
   */
  public static void spinUpHttpServer(RequestHandler callback) throws IOException {
    HttpServer httpServer = HttpServer.getInstance();
    httpServer.setRequestCallback(callback);

    httpServer.listen(-1);
  }
}

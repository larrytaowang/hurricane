package com.hurricane.hurricane.utility;

import com.hurricane.hurricane.http.HttpServer;
import com.hurricane.hurricane.web.Application;
import java.io.IOException;


public class HttpUtil {
  /**
   * Binds the HTTP server to an ephemeral port and listens to new connections.
   * @param application application whose request handler will be run when HTTP server finishes parsing HTTP request
   * @throws IOException Some IO errors in bind and listen operations.
   */
  public static void spinUpHttpServer(Application application) throws IOException {
    HttpServer httpServer = HttpServer.getInstance();
    httpServer.setApplication(application);

    httpServer.listen(-1);
  }
}

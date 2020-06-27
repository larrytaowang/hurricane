package com.hurricane.hurricane.http;

import com.hurricane.hurricane.tcp.TcpAcceptManager;
import com.hurricane.hurricane.tcp.TcpServer;
import com.hurricane.hurricane.tcp.connection.TcpConnection;
import com.hurricane.hurricane.web.RequestHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;


/**
 * @author larrytaowang
 *
 * A non-blocking, single-threaded HTTP server, which is essentially a TcpServer with READ/WRITE callbacks that follows
 * HTTP protocols.
 *
 * When a TCP connection is established, the READ callback is set to wait for HTTP header data, that is, data with
 * delimiter "\r\n". When the HTTP header data is received and processed, a READ callback for parsing HTTP body will
 * be set if necessary. Similarly, when HTTP server wants to send a response to the client, it needs to prepare the
 * response data and set the WRITE callbacks accordingly.
 */
public class HttpServer {
  /**
   * A single static instance of HttpServer
   */
  private static HttpServer httpServer;

  /**
   * Callback will be executed when finishing parsing Http request
   */
  private RequestHandler requestCallback;

  private HttpServer() {
  }

  /**
   * Get a global instance of HttpServer
   * @return a global HttpServer instance
   */
  public static HttpServer getInstance() {
    if (httpServer == null) {
      httpServer = new HttpServer();
    }

    return httpServer;
  }

  /**
   * Bind the server to desired port and address. Use the requestCallback to handle the new TCP connection.
   * @param port port that this server will bind to. if "-1" use an ephemeral port.
   * @param address Address of this server
   * @throws IOException Some IO errors when binding the server
   */
  public void listen(int port, String address) throws IOException {
    var httpAcceptManager = new TcpAcceptManager() {
      @Override
      protected void setUpTcpConnectionHandler(TcpConnection tcpConnection) {
        var newHttpConnection = new HttpConnection(tcpConnection, requestCallback);
        newHttpConnection.activate();
      }
    };

    TcpServer.init(httpAcceptManager);

    // Bind the server
    if (port == -1) {
      TcpServer.bind(null);
    } else {
      var inetAddress = InetAddress.getByName(address);
      var endPoint = new InetSocketAddress(inetAddress, port);
      TcpServer.bind(endPoint);
    }
  }

  /**
   * Bind the server to desired port. Use the requestCallback to handle the new TCP connection.
   * @param port port that this server will bind to. if "-1" use an ephemeral port.
   * @throws IOException Some IO errors when binding the server
   */
  public void listen(int port) throws IOException {
    listen(port, "localhost");
  }

  public void setRequestCallback(RequestHandler requestCallback) {
    this.requestCallback = requestCallback;
  }
}

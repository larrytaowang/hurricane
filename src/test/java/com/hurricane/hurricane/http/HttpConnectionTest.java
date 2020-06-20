package com.hurricane.hurricane.http;

import com.hurricane.hurricane.common.Constant;
import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.utility.TcpUtil;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class HttpConnectionTest {

  /**
   *  We will create this count of clients for testing.
   */
  public static final int CLIENT_COUNT = 10;

  /**
   * A list of clients we created for testing.
   */
  private List<Socket> clients;

  /**
   * Connections that the http server has served. This is added by the request callback.
   */
  private List<HttpConnection> servedConnections;

  /**
   * We need to spin up a bunch of clients interacting with servers. A client is wrapped in a runnable and submitted
   * to the executor server.
   */
  private ExecutorService executeService;

  /**
   * This latch is used for sync up with threads in pool. All threads should finish before main thread exists.
   */
  private CountDownLatch latch;

  @Before
  public void setUp() {
    this.executeService = Executors.newFixedThreadPool(CLIENT_COUNT + 1);
    latch = new CountDownLatch(CLIENT_COUNT);
    servedConnections = new ArrayList<>();
  }

  /**
   * Test when the clients send Http request with only headers to the server, the server should be able to understand it.
   */
  @Test
  public void parseHttpHeader() throws IOException, InterruptedException {
    // Set up HTTP server
    HttpRequestCallback callback = (connection, request) -> {
      servedConnections.add(connection);
      if (servedConnections.size() == CLIENT_COUNT) {
        EventLoop.getInstance().stop();
      }
    };
    spinUpHttpServer(callback);
    clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);

    // Clients send data of test HTTP header to the HTTP server
    var httpRequestString = "GET /hurricane/test/ HTTP/1.1\r\n" + "Cookie: PHPSESSID=r2t5uvjq435r4q7ib3vtdjq120\r\n"
        + "Cache-Control: no-cache\r\n" + "\r\n";
    var httpRequestData = httpRequestString.getBytes(StandardCharsets.UTF_8);
    for (var client : clients) {
      executeService.submit(() -> {
        TcpUtil.clientSendData(client, httpRequestData);
        latch.countDown();
        return null;
      });
    }

    // Start the IO event and check if the server has received and parsed the data correctly
    EventLoop.getInstance().start();
    Assert.assertEquals(CLIENT_COUNT, servedConnections.size());
    for (var connection : servedConnections) {
      var httpRequest = connection.getHttpRequest();
      Assert.assertEquals("GET", httpRequest.getMethod());
      Assert.assertEquals("/hurricane/test/", httpRequest.getUri());
      Assert.assertEquals("HTTP/1.1", httpRequest.getVersion());

      var httpHeaders = httpRequest.getHttpHeaders();
      Assert.assertEquals("PHPSESSID=r2t5uvjq435r4q7ib3vtdjq120", httpHeaders.getValues("Cookie"));
      Assert.assertEquals("no-cache", httpHeaders.getValues("Cache-Control"));
    }

    // wait for all threads in the pool finish
    latch.await();
  }

  /**
   * Test when the HTTP server understand request with "application/x-www-form-urlencoded" body correctly
   */
  @Test
  public void parseHttpUrlEncodedBody() throws IOException, InterruptedException {
    // Set up HTTP server
    HttpRequestCallback callback = (connection, request) -> {
      servedConnections.add(connection);
      if (servedConnections.size() == CLIENT_COUNT) {
        EventLoop.getInstance().stop();
      }
    };
    spinUpHttpServer(callback);
    clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);

    // Clients send data of test HTTP request header + body to the HTTP server
    var httpRequestString =
        "POST /test HTTP/1.1\r\n" + "Host: foo.example\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Content-Length: 27\r\n" + "\r\n" + "field1=value1&field2=value2";
    var httpRequestData = httpRequestString.getBytes(StandardCharsets.UTF_8);
    for (var client : clients) {
      executeService.submit(() -> {
        TcpUtil.clientSendData(client, httpRequestData);
        latch.countDown();
        return null;
      });
    }

    // Start the IO event and check if the server has received and parsed the data correctly
    EventLoop.getInstance().start();
    Assert.assertEquals(CLIENT_COUNT, servedConnections.size());
    for (var connection : servedConnections) {
      var arguments = connection.getHttpRequest().getHttpBody().getArguments();
      Assert.assertEquals(2, arguments.size());
      Assert.assertEquals("value1", arguments.get("field1"));
      Assert.assertEquals("value2", arguments.get("field2"));
    }

    // wait for all threads in the pool finish
    latch.await();
  }

  /**
   * Test when the HTTP server understand 100-continue request correctly and send response back
   */
  @Test
  public void parseHttpHeaderOneHundredContinue() throws IOException, InterruptedException {
    // Set up HTTP server
    spinUpHttpServer(null);
    clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);

    // Clients send HTTP header to the HTTP server and expect response
    var httpRequestString =
        "PUT /somewhere/fun HTTP/1.1\r\n" + "Host: origin.example.com\r\n" + "Content-Length: 12345\r\n"
            + "Expect: 100-continue\r\n" + "\r\n";
    var httpRequestData = httpRequestString.getBytes(StandardCharsets.UTF_8);
    for (var client : clients) {
      executeService.submit(() -> {
        // Client send HTTP header to server
        TcpUtil.clientSendData(client, httpRequestData);

        // Client should receive HTTP response for 100-continue
        var response = Constant.HTTP_100_CONTINUE_RESPONSE.getBytes(StandardCharsets.UTF_8);
        TcpUtil.clientShouldReceiveData(client, response);

        latch.countDown();
        if (latch.getCount() == 0) {
          EventLoop.getInstance().stop();
        }
        return null;
      });
    }

    // Start the IO event
    EventLoop.getInstance().start();

    // wait for all threads in the pool finish
    latch.await();
  }

  /**
   * Binds the HTTP server to an ephemeral port and listens to new connections.
   * @param callback callback that will be run when HTTP server finishes parsing HTTP request
   * @throws IOException Some IO errors in bind and listen operations.
   */
  private void spinUpHttpServer(HttpRequestCallback callback) throws IOException {
    HttpServer httpServer = HttpServer.getInstance();
    httpServer.setRequestCallback(callback);

    httpServer.listen(-1);
  }
}
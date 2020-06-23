package com.hurricane.hurricane.web;

import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.http.HttpException;
import com.hurricane.hurricane.http.HttpRequest;
import com.hurricane.hurricane.utility.TcpUtil;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.hurricane.hurricane.utility.HttpUtil.*;


public class RequestHandlerTest {

  /**
   * We will create this count of clients for testing.
   */
  public static final int CLIENT_COUNT = 10;

  /**
   * A list of clients we created for testing.
   */
  private List<Socket> clients;

  /**
   * Number of the Http request that has been served
   */
  private int servedClientCount;

  /**
   * We need to spin up a bunch of clients interacting with servers. A client is wrapped in a runnable and submitted to
   * the executor server.
   */
  private ExecutorService executeService;

  /**
   * This latch is used for sync up with threads in pool. All threads should finish before main thread exists.
   */
  private CountDownLatch latch;

  @Before
  public void setUp() {
    this.executeService = Executors.newFixedThreadPool(CLIENT_COUNT + 1);
    this.latch = new CountDownLatch(CLIENT_COUNT);
    this.servedClientCount = 0;
  }

  @Test
  public void handleGetMethod() throws IOException, InterruptedException {
    var response = "Hello World!";

    // Set up HTTP server
    RequestHandler requestHandler = new RequestHandler() {
      @Override
      protected void handleGetMethod(HttpRequest request) throws HttpException {
        write(response);
        servedClientCount += 1;
      }
    };

    var application = new Application(Collections.singletonList(new UrlSpec(".*", requestHandler)));
    spinUpHttpServer(application);
    clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);

    // Clients send data of test HTTP header to the HTTP server
    var httpRequestString = "GET /hurricane/test/ HTTP/1.1\r\n\r\n";
    var httpRequestData = httpRequestString.getBytes(StandardCharsets.UTF_8);

    var expectedResponse = "HTTP/1.1 200 OK\r\n" + "Content-Length: 12\r\n\r\n" + response;
    for (var client : clients) {
      executeService.submit(() -> {
        System.out.println("Port = " + client.getLocalPort());
        TcpUtil.clientSendData(client, httpRequestData);
        TcpUtil.clientShouldReceiveData(client, expectedResponse);
        latch.countDown();
        if (latch.getCount() == 0) {
          EventLoop.getInstance().stop();
        }

        return null;
      });
    }

    // Start the IO event and check if the server has received and parsed the data correctly
    EventLoop.getInstance().start();
    Assert.assertEquals(CLIENT_COUNT, servedClientCount);

    // wait for all threads in the pool finish
    latch.await();
  }
}
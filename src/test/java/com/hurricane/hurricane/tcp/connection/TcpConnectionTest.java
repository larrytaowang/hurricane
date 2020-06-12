package com.hurricane.hurricane.tcp.connection;

import com.google.common.primitives.Bytes;
import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.common.TimeEvent;
import com.hurricane.hurricane.tcp.TcpServer;
import com.hurricane.hurricane.tcp.TcpAcceptManager;
import com.hurricane.hurricane.tcp.callback.TcpFlushHandler;
import com.hurricane.hurricane.tcp.callback.TcpCallback;
import com.hurricane.hurricane.tcp.callback.TcpReadBytesHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadDelimiterHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadHandler;
import com.hurricane.hurricane.tcp.callback.TcpWriteHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TcpConnectionTest {
  private final static Logger logger = Logger.getLogger(TcpConnectionTest.class);

  /**
   * A delimiter is necessary for testing readDelimiter callback.
   */
  private final String delimiter = "\n";

  /**
   * Test data that will be transferred between clients and server
   */
  private final String testString = "Some test text here" + delimiter;

  /**
   * Test bytes that will be transferred between clients and server
   */
  private final byte[] testBytes = testString.getBytes(StandardCharsets.UTF_8);

  /**
   * A global event loop instance.
   */
  private EventLoop eventLoop;

  /**
   * We need to spin up a bunch of clients interacting with servers. A client is wrapped in a runnable and submitted
   * to the executor server.
   */
  private ExecutorService executeService;

  /**
   * After a read event happens, the data read from each client will be store in this list, to compare with the original
   * data.
   */
  private List<Byte> serverReadData;

  /**
   * We will create this count of clients for testing.
   */
  private final int clientCount = 1;

  /**
   * A list of clients we created for testing.
   */
  private List<Socket> clients;

  /**
   * We will increase this count after a callback is triggered. Ideally this count should be equal to the client count
   * in the end.
   */
  private int callbackTriggeredCount;

  /**
   * Callback that will be triggered if needed when there is a READ event.
   */
  private TcpCallback postReadEventCallback;

  @Before
  public void setUp() {
    this.serverReadData = new ArrayList<>();
    this.executeService = Executors.newFixedThreadPool(clientCount + 1);
    this.eventLoop = EventLoop.getInstance();

    this.postReadEventCallback = (handler, args) -> {
      appendServerReadData((byte[]) args[0]);
      callbackTriggeredCount += 1;
      if (callbackTriggeredCount == clientCount) {
        eventLoop.stop();
      }
    };
  }

  /**
   * Each client will send test data to the server. After required count of bytes are received from one client, the
   * callback will be triggered and that required count of data will be appended to 'serverReadData'. When callback
   * of all the clients are triggered, the event loop will be stored and we can check if the data in 'serverReadData'
   * is as expected.
   */
  @Test
  public void readBytesEvent() throws IOException {
    // Set up read handler for new accepted connection
    final int requiredBytesCount = 10;
    var callback = new TcpReadBytesHandler(requiredBytesCount, postReadEventCallback);
    setUpTcpAcceptHandler(callback, null);

    // Spin up clients and send data to server in parallel
    prepareConnectedClients();
    clientSendData();

    // Start the event loop
    eventLoop.start();

    // When the event loop finishes, check if the server receives data as expected
    final byte[] subTestBytes = Arrays.copyOfRange(testBytes, 0, requiredBytesCount);
    final byte[] expectedBytes = repeatArray(subTestBytes, clientCount);
    Assert.assertArrayEquals(expectedBytes, Bytes.toArray(serverReadData));
  }

  /**
   * Each client will send test data to the server. After delimiter is received from one client, the callback will be
   * triggered and that all received data before delimiter (included) will be appended to 'serverReadData'. When
   * callback of all the clients are triggered, the event loop will be stored and we can check if the data in
   * 'serverReadData' is as expected.
   */
  @Test
  public void readDelimiterEvent() throws IOException {
    // Set up read handler for new accepted connection
    var callback = new TcpReadDelimiterHandler(delimiter, postReadEventCallback);
    setUpTcpAcceptHandler(callback, null);

    // Spin up clients and send data to server in parallel
    prepareConnectedClients();
    clientSendData();

    // Start the event loop
    eventLoop.start();

    // When the event loop finishes, check if the server receives data as expected
    final byte[] expectedBytes = repeatArray(testBytes, clientCount);
    Assert.assertArrayEquals(expectedBytes, Bytes.toArray(serverReadData));
  }

  @Test
  public void writeEvent() throws IOException {
    TcpCallback postCallback = (handler, args) -> {
      callbackTriggeredCount += 1;
      if (callbackTriggeredCount == clientCount) {
        eventLoop.stop();
      }
    };

    // Set up write handler for new accepted connection
    var callback = new TcpFlushHandler(postCallback);
    setUpTcpAcceptHandler(null, callback);

    // Spin up clients and send data to server in parallel
    final byte[] testBytes = testString.getBytes(StandardCharsets.UTF_8);
    prepareConnectedClients();
    clientReceiveData();

    eventLoop.addTimeEvent(new TimeEvent(System.currentTimeMillis() + 100, args -> {
      logger.info("[Server] Callback");
      for (var connection : eventLoop.getClientConnections().values()) {
        logger.info("[Server] Write test data to tcp connection");
        connection.prepareWriteData(testBytes);
      }
    }));

    // Start the event loop
    eventLoop.start();

    Assert.assertEquals(clientCount, callbackTriggeredCount);
  }

  /**
   * Each client will send data to server in parallel.
   */
  private void clientSendData() {
    for (var client : clients) {
      executeService.submit(() -> {
        OutputStream outputStream = client.getOutputStream();
        logger.info("[Client-" + clients.indexOf(client) + "] Send test bytes, count = " + testBytes.length);
        outputStream.write(testBytes);
        return testBytes;
      });
    }
  }

  /**
   * Each client will receive data from server in parallel.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void clientReceiveData() {

    for (var client : clients) {
      executeService.submit(() -> {
        InputStream inputStream = client.getInputStream();

        // Receive the test string from the server
        int totalBytesReceived = 0;
        byte[] receivedData = new byte[testBytes.length];
        while (totalBytesReceived < testBytes.length) {
          inputStream.read(receivedData, totalBytesReceived, testBytes.length - totalBytesReceived);
        }

        logger.info("[Client-" + clients.indexOf(client) + "] Received completed test data from server");
        Assert.assertArrayEquals(testBytes, receivedData);
        return null;
      });
    }
  }

  /**
   * This is used for read test. When a server receives data and callback is triggered, the callback can use this
   * function to store the received data in the test class via this method.
   * @param serverReadData Data that the callback wants to store in this test class.
   */
  private void appendServerReadData(byte[] serverReadData) {
    for (var oneByte : serverReadData) {
      this.serverReadData.add(oneByte);
    }
  }

  /**
   * Repeat an array n times
   * @param bytes byte array we want to repeat
   * @param repeatCount repeat count of an array
   * @return new array with desired repeat count
   */
  private byte[] repeatArray(byte[] bytes, int repeatCount) {
    byte[] repeatedBytes = new byte[bytes.length * repeatCount];
    for (int i = 0; i < repeatedBytes.length; i++) {
      repeatedBytes[i] = bytes[i % bytes.length];
    }
    return repeatedBytes;
  }

  /**
   * Prepare a bunch of clients which successfully connect to the server.
   * @throws IOException some IO errors in TCP connecting operations
   */
  private void prepareConnectedClients() throws IOException {
    clients = new ArrayList<>();
    var serverSocket = TcpServer.getServerSocketChannel().socket();
    for (int i = 0; i < clientCount; i++) {
      var client = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
      this.clients.add(client);
      logger.info("[Client-" + i + "] Connect to server");
    }
  }

  /**
   * Set up accept handler for new connection.
   * @param callback callback that will be associated with the READ event for the new connections.
   * @throws IOException some IO errors in TCP connecting operations
   */
  private void setUpTcpAcceptHandler(TcpReadHandler callback, TcpWriteHandler tcpWriteHandler) throws IOException {
    var acceptManager = new TcpAcceptManager(callback, tcpWriteHandler);
    TcpServer.init(acceptManager);
    TcpServer.bind(null);
  }
}
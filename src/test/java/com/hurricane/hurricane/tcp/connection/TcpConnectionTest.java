package com.hurricane.hurricane.tcp.connection;

import com.google.common.primitives.Bytes;
import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.common.TimeEvent;
import com.hurricane.hurricane.tcp.TcpAcceptManager;
import com.hurricane.hurricane.tcp.TcpServer;
import com.hurricane.hurricane.tcp.callback.TcpCallback;
import com.hurricane.hurricane.tcp.callback.TcpFlushHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadBytesHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadDelimiterHandler;
import com.hurricane.hurricane.tcp.callback.TcpReadHandler;
import com.hurricane.hurricane.tcp.callback.TcpWriteHandler;
import com.hurricane.hurricane.utility.TcpUtil;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
  public static final String DELIMITER = "\n";

  /**
   * Test data that will be transferred between clients and server
   */
  public static final String TEST_STRING = "Some test text here" + DELIMITER;

  /**
   * Test bytes that will be transferred between clients and server
   */
  public static final byte[] TEST_BYTES = TEST_STRING.getBytes(StandardCharsets.UTF_8);

  /**
   * We will create this count of clients for testing.
   */
  public static final int CLIENT_COUNT = 10;

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

  /**
   * This latch is used for sync up with threads in pool. All threads should finish before main thread exists.
   */
  private CountDownLatch latch;

  @Before
  public void setUp() {
    this.serverReadData = new ArrayList<>();
    this.executeService = Executors.newFixedThreadPool(CLIENT_COUNT + 1);
    this.eventLoop = EventLoop.getInstance();
    this.latch = new CountDownLatch(CLIENT_COUNT);

    this.postReadEventCallback = (handler, args) -> {
      appendServerReadData((byte[]) args[0]);
      callbackTriggeredCount += 1;
      if (callbackTriggeredCount == CLIENT_COUNT) {
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
  public void readBytesEvent() throws IOException, InterruptedException {
    // Set up read handler for new accepted connection
    final int requiredBytesCount = 10;
    var callback = new TcpReadBytesHandler(requiredBytesCount, postReadEventCallback);
    setUpTcpAcceptHandler(callback, null);

    // Spin up clients and send data to server in parallel
    this.clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);
    clientsSendDataInParallel(latch);

    // Start the event loop
    eventLoop.start();

    // When the event loop finishes, check if the server receives data as expected
    final byte[] subTestBytes = Arrays.copyOfRange(TEST_BYTES, 0, requiredBytesCount);
    final byte[] expectedBytes = repeatArray(subTestBytes, CLIENT_COUNT);
    Assert.assertArrayEquals(expectedBytes, Bytes.toArray(serverReadData));

    // wait for all threads in the pool finish
    latch.await();
  }

  /**
   * Each client will send test data to the server. After delimiter is received from one client, the callback will be
   * triggered and that all received data before delimiter (included) will be appended to 'serverReadData'. When
   * callback of all the clients are triggered, the event loop will be stored and we can check if the data in
   * 'serverReadData' is as expected.
   */
  @Test
  public void readDelimiterEvent() throws IOException, InterruptedException {
    // Set up read handler for new accepted connection
    var callback = new TcpReadDelimiterHandler(DELIMITER, postReadEventCallback);
    setUpTcpAcceptHandler(callback, null);

    // Spin up clients and send data to server in parallel
    this.clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);
    clientsSendDataInParallel(latch);

    // Start the event loop
    eventLoop.start();

    // When the event loop finishes, check if the server receives data as expected
    final byte[] expectedBytes = repeatArray(TEST_BYTES, CLIENT_COUNT);
    Assert.assertArrayEquals(expectedBytes, Bytes.toArray(serverReadData));

    // wait for all threads in the pool finish
    latch.await();
  }

  @Test
  public void writeEvent() throws IOException, InterruptedException {
    TcpCallback postCallback = (handler, args) -> {
      callbackTriggeredCount += 1;
      if (callbackTriggeredCount == CLIENT_COUNT) {
        eventLoop.stop();
      }
    };

    // Set up write handler for new accepted connection
    var callback = new TcpFlushHandler(postCallback);
    setUpTcpAcceptHandler(null, callback);

    // Spin up clients and send data to server in parallel
    final byte[] testBytes = TEST_STRING.getBytes(StandardCharsets.UTF_8);
    this.clients = TcpUtil.prepareConnectedClients(CLIENT_COUNT);

    clientsReceiveDataInParallel(latch);

    eventLoop.addTimeEvent(new TimeEvent(System.currentTimeMillis() + 100, args -> {
      logger.info("[Server] Callback");
      for (var connection : eventLoop.getClientConnections().values()) {
        logger.info("[Server] Write test data to tcp connection");
        connection.prepareWriteData(testBytes);
      }
    }));

    // Start the event loop
    eventLoop.start();

    Assert.assertEquals(CLIENT_COUNT, callbackTriggeredCount);

    // wait for all threads in the pool finish
    latch.await();
  }

  /**
   * Each client start to send test data. When expected data is sent, decrease the latch.
   * @param latch this is used to make sure main thread does not exit before all threads in pool finish.
   */
  private void clientsSendDataInParallel(CountDownLatch latch) {
    for (var client : clients) {
      executeService.submit(() -> {
        TcpUtil.clientSendData(client, TEST_BYTES);
        latch.countDown();
        return TEST_BYTES;
      });
    }
  }

  /**
   * Each client start to receive test data. When expected data is received, decrease the latch.
   * @param latch this is used to make sure main thread does not exit before all threads in pool finish.
   */
  private void clientsReceiveDataInParallel(CountDownLatch latch) {
    for (var client : clients) {
      executeService.submit(() -> {
        TcpUtil.clientShouldReceiveData(client, TEST_BYTES);
        latch.countDown();
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
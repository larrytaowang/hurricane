package com.hurricane.hurricane.utility;

import com.hurricane.hurricane.tcp.TcpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.Assert;


public class TcpUtil {
  private final static Logger logger = Logger.getLogger(TcpUtil.class);

  /**
   * Prepare a bunch of clients which successfully connect to the server.
   * @param clientCount number of clients created
   * @throws IOException some IO errors in TCP connecting operations
   * @return clients that are ready to send or receive data
   */
  public static List<Socket> prepareConnectedClients(int clientCount) throws IOException {
    var clients = new ArrayList<Socket>();
    var serverSocket = TcpServer.getServerSocketChannel().socket();
    for (int i = 0; i < clientCount; i++) {
      var client = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
      clients.add(client);
      logger.info("[Client-" + i + "] Connect to server");
    }

    return clients;
  }

  /**
   * The client send given data
   * @param client client socket
   * @param data data to send
   * @throws IOException Some IO errors when sending TCP data
   */
  public static void clientSendData(Socket client, byte[] data) throws IOException {
    OutputStream outputStream = client.getOutputStream();
    logger.info("[Client] Send test bytes, count = " + data.length);
    outputStream.write(data);
  }

  /**
   * The client should receive expected data
   * @param client client socket
   * @param expectedData expected data to receive
   * @throws IOException Some IO errors when receiving TCP data
   */
  public static void clientShouldReceiveData(Socket client, byte[] expectedData) throws IOException {
    InputStream inputStream = client.getInputStream();

    // Receive the test string from the server
    var totalReceivedBytes = 0;
    var receivedData = new byte[expectedData.length];
    while (totalReceivedBytes < expectedData.length) {
      var receivedBytesCount =
          inputStream.read(receivedData, totalReceivedBytes, expectedData.length - totalReceivedBytes);
      totalReceivedBytes += receivedBytesCount;
      logger.info("[Client] Received [" + totalReceivedBytes + "] bytes in total.");
    }

    Assert.assertArrayEquals(expectedData, receivedData);
    logger.info("[Client] Received completed test data from server");
  }
}

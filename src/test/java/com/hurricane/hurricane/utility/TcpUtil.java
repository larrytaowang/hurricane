package com.hurricane.hurricane.utility;

import com.hurricane.hurricane.tcp.TcpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.Assert;


public class TcpUtil {
  private final static Logger logger = Logger.getLogger(TcpUtil.class);

  /**
   * Prepare a bunch of clients which successfully connect to the server.
   *
   * @param clientCount number of clients created
   * @return clients that are ready to send or receive data
   * @throws IOException some IO errors in TCP connecting operations
   */
  public static List<Socket> prepareConnectedClients(int clientCount) throws IOException {
    var clients = new ArrayList<Socket>();
    var serverSocket = TcpServer.getServerSocketChannel().socket();
    for (int i = 0; i < clientCount; i++) {
      var client = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
      clients.add(client);
      logger.info("[Client-" + i + "] Connect to server, port = " + client.getLocalPort());
    }

    return clients;
  }

  /**
   * The client send given data
   *
   * @param client client socket
   * @param data   data to send
   * @throws IOException Some IO errors when sending TCP data
   */
  public static void clientSendData(Socket client, byte[] data) throws IOException {
    OutputStream outputStream = client.getOutputStream();
    logger.info("[Client] Send test bytes, count = " + data.length + ", port = " + client.getLocalPort());
    outputStream.write(data);
  }

  /**
   * The client should receive expected data
   *
   * @param client       client socket
   * @param expectedData expected data to receive
   * @throws IOException Some IO errors when receiving TCP data
   */
  public static void clientShouldReceiveData(Socket client, String expectedData) throws IOException {
    InputStream inputStream = client.getInputStream();
    var expectedBytes = expectedData.getBytes(StandardCharsets.UTF_8);

    // Receive the test string from the server
    var totalReceivedBytes = 0;
    var receivedData = new byte[expectedBytes.length];
    while (totalReceivedBytes < expectedBytes.length) {
      var receivedBytesCount =
          inputStream.read(receivedData, totalReceivedBytes, expectedBytes.length - totalReceivedBytes);
      totalReceivedBytes += receivedBytesCount;
      logger.info("[Client] Received [" + totalReceivedBytes + "] bytes." + ", port = " + client.getLocalPort());
    }

    var receivedString = new String(receivedData, StandardCharsets.UTF_8);
    logger.info("[Client] Received complete response = " + receivedString + ", port = " + client.getLocalPort());
    Assert.assertEquals(expectedData, receivedString);
  }
}

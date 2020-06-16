package com.hurricane.hurricane.tcp;

import com.hurricane.hurricane.tcp.connection.TcpConnection;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;


/**
 * @author larrytaowang
 *
 * This class processes server socket ACCEPT event. After acceping, the client TCP connection will be register with the
 * desired read write handler.
 */
public abstract class TcpAcceptManager {
  /**
   * Handle ACCEPT Socket IO event. The subclass should define how to interact with the client TCP connection.
   * @throws IOException Some IO errors happen in accepting the client socket
   */
  protected void handleAcceptEvent() throws IOException {
    // Accept client channel and register to the selector
    var clientChannel = ((ServerSocketChannel) TcpServer.getServerKey().channel()).accept();
    clientChannel.configureBlocking(false);
    var selector = TcpServer.getServerKey().selector();
    var clientKey = clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

    // Set read and write callback for the client channel
    var clientConnection = new TcpConnection(clientKey);
    setUpTcpConnectionHandler(clientConnection);
  }

  /**
   * After a client TCP connection is accepted, this function defines how the server interact with the client connection.
   * @param connection client TCP connection
   */
  abstract protected void setUpTcpConnectionHandler(TcpConnection connection);
}

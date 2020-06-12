package com.hurricane.hurricane.tcp;

import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.tcp.callback.TcpReadHandler;
import com.hurricane.hurricane.tcp.callback.TcpWriteHandler;
import com.hurricane.hurricane.tcp.connection.TcpReadManager;
import com.hurricane.hurricane.tcp.connection.TcpConnection;
import com.hurricane.hurricane.tcp.connection.TcpWriteManager;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * This class processes server socket ACCEPT event. After acceping, the client TCP connection will be register with the
 * desired read write handler.
 */
public class TcpAcceptManager {
  private final static Logger logger = Logger.getLogger(TcpAcceptManager.class);

  /**
   * Read Callback attached to accepted Tcp connection
   */
  private TcpReadHandler readHandler;

  /**
   * Write Callback attached to accepted Tcp connection
   */
  private TcpWriteHandler writeHandler;

  public TcpAcceptManager(TcpReadHandler readHandler, TcpWriteHandler writeHandler) {
    this.readHandler = readHandler;
    this.writeHandler = writeHandler;
  }

  /**
   * Handle ACCEPT Socket IO event. The write handler and read handler will be attached to the new client connection.
   * @throws IOException Some IO errors happen in accepting the client socket
   */
  protected void handleAcceptEvent() throws IOException {
    // Accept client channel and register to the selector
    var clientChannel = ((ServerSocketChannel) TcpServer.getServerKey().channel()).accept();
    clientChannel.configureBlocking(false);
    var clientKey = clientChannel.register(TcpServer.getServerKey().selector(), SelectionKey.OP_READ);

    // Set read and write callback for the client channel
    var clientConnection = new TcpConnection(clientKey, new TcpReadManager(), new TcpWriteManager());
    clientConnection.setReadHandler(readHandler);
    clientConnection.setWriteHandler(writeHandler);
    EventLoop.getInstance().registerTcpConnection(clientKey, clientConnection);

    logger.info("Accept and add read callback for client Key = " + clientKey);
  }
}

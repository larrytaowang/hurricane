package com.hurricane.hurricane.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 *
 */
public class TcpServer {
  private final static Logger logger = Logger.getLogger(TcpServer.class);

  /**
   * A selector for all network IO events
   */
  private static Selector selector;

  /**
   * Selection key of this Tcp server
   */
  private static SelectionKey serverKey;

  /**
   * Socket channel of this TCP server
   */
  private static ServerSocketChannel serverSocketChannel;

  /**
   * Accept manager of this Tcp server, which defines what Read/Write Manager will be attached to the future client
   * connection.
   */
  private static TcpAcceptManager tcpAcceptManager;

  /**
   * Initialize the Tcp server
   * @param manager Accept manager that will be used for handling the Tcp server ACCEPT event
   * @throws IOException IO errors when initializing selector, socket channel and channel register
   */
  public static void init(TcpAcceptManager manager) throws IOException {
    selector = Selector.open();
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverKey = serverSocketChannel.register(TcpServer.getSelector(), SelectionKey.OP_ACCEPT);
    tcpAcceptManager = manager;
  }

  /**
   * Bind the Tcp server to socket address
   * @param address socket address this Tcp server will bind to. If null, an ephemeral port will be used.
   * @throws IOException IO errors that happen during binding operations
   */
  public static void bind(SocketAddress address) throws IOException {
    serverSocketChannel.socket().bind(address);
  }

  /**
   * When an accept event is ready for server socket, process this accept event.
   */
  public static void handleServerSocketEvent() {
    var key = TcpServer.getServerKey();
    if (key.isAcceptable()) {
      try {
        TcpServer.getTcpAcceptManager().handleAcceptEvent();
      } catch (IOException e) {
        logger.warn("Failed to handle accept event for server socket", e);
      }
    }
  }

  public static ServerSocketChannel getServerSocketChannel() {
    return serverSocketChannel;
  }

  public static TcpAcceptManager getTcpAcceptManager() {
    return tcpAcceptManager;
  }

  public static SelectionKey getServerKey() {
    return serverKey;
  }

  public static Selector getSelector() {
    return selector;
  }
}

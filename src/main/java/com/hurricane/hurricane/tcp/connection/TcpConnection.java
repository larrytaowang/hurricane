package com.hurricane.hurricane.tcp.connection;

import com.hurricane.hurricane.common.TcpCallback;
import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.tcp.callback.TcpReadHandler;
import com.hurricane.hurricane.tcp.callback.TcpWriteHandler;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * When a client is accepted, an associated TcpConnection will be registered in event loop with its selection key. This
 * class is used to maintain client read write cache and handler.
 */
public class TcpConnection {
  private final static Logger logger = Logger.getLogger(TcpConnection.class);

  /**
   * Default max size for readCache and writeCache
   */
  public static final int DEFAULT_MAX_CACHE_SIZE = 104857600;

  /**
   * Default size for byte buffer
   */
  public static final int DEFAULT_BYTE_BUFFER_SIZE = 4096;

  /**
   * Each selection key should have one handler.
   */
  protected SelectionKey key;

  /**
   * Client Socket Channel associated with the given key
   */
  protected SocketChannel socketChannel;

  /**
   * A global eventLoop instance.
   */
  protected EventLoop eventLoop;

  /**
   * Manager process client READ event and maintains read cache.
   */
  private TcpReadManager readManager;

  /**
   * Manager process client WRITE event and maintains read cache.
   */
  private TcpWriteManager writeManager;

  /**
   * This handler will be triggered after client READ event if needed.
   */
  private TcpReadHandler readHandler;

  /**
   * This handler will be triggered after client WRITE event if needed.
   */
  private TcpWriteHandler writeHandler;

  /**
   * Tcp Callback that will be triggered when this client Tcp connection is closed.
   */
  private TcpCallback closeCallback;

  public TcpConnection(SelectionKey key) {
    this(key, new TcpReadManager(), new TcpWriteManager());
  }

  public TcpConnection(SelectionKey key, TcpReadManager readManager) {
    this(key, readManager, new TcpWriteManager());
  }

  public TcpConnection(SelectionKey key, TcpReadManager readManager, TcpWriteManager writeManager) {
    this.key = key;
    if (key.channel() instanceof SocketChannel) {
      this.socketChannel = (SocketChannel) key.channel();
    }

    this.eventLoop = EventLoop.getInstance();
    this.readManager = readManager;
    this.writeManager = writeManager;
  }

  /**
   * Handle IO event for this Tcp connection
   */
  public void handleClientSocketEvent() throws IOException {
    if (!key.isValid()) {
      logger.warn("Got events for invalid key = " + key);
      closeTcpConnection();
      return;
    }

    if (key.isReadable()) {
      readManager.handleReadEvent(this);
    }

    if (key.isValid() && key.isWritable()) {
      writeManager.handleWriteEvent(this);
    }
  }

  /**
   * Close this tcp connection. Also, remove the handler in eventloop, and remove the key from select
   * interested list. Run closeCallback if needed.
   */
  protected void closeTcpConnection() {
    eventLoop.deregisterTcpConnection(key);
    key.cancel();

    try {
      key.channel().close();
    } catch (IOException e) {
      logger.warn("Failed to close channel = " + key.channel(), e);
    }

    if (closeCallback != null) {
      closeCallback.run(new Object[0]);
    }
  }

  /**
   * Check if the current Tcp connection is open
   * @return if the Tcp connection is open
   */
  protected boolean isConnectionClosed() {
    if (!socketChannel.isOpen()) {
      logger.warn("Closed channel = " + socketChannel);
      return true;
    }

    return false;
  }

  /**
   * Append new data we want to write to the write cache.
   * @param data new data we want to write to the socket channel.
   */
  public void prepareWriteData(byte[] data) {
    key.interestOpsOr(SelectionKey.OP_WRITE);
    writeManager.writeDataToCache(data);
    if (writeManager.isCacheOverflow()) {
      logger.warn("Reached maximum read cache size, close channel = " + socketChannel);
      closeTcpConnection();
    }
  }

  /**
   * Set Tcp Read handler for this Tcp connection. When new handler is set, we should make sure this key registers
   * the READ event.
   * @param readHandler set new handler for this Tcp connection
   */
  public void setReadHandler(TcpReadHandler readHandler) {
    if (isConnectionClosed()) {
      return;
    }

    this.readHandler = readHandler;
    this.key.interestOpsOr(SelectionKey.OP_READ);
  }

  /**
   * Set Tcp Write handler for this Tcp connection. We don't need to add interest in WRITE operation, prepareWriteData()
   * is better place to do that
   * @param writeHandler TCP write handler
   */
  public void setWriteHandler(TcpWriteHandler writeHandler) {
    if (isConnectionClosed()) {
      return;
    }

    this.writeHandler = writeHandler;
  }

  public TcpReadHandler getReadHandler() {
    return readHandler;
  }

  public TcpReadManager getReadManager() {
    return readManager;
  }

  public TcpWriteHandler getWriteHandler() {
    return writeHandler;
  }

  public TcpWriteManager getWriteManager() {
    return writeManager;
  }
}

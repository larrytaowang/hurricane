package com.hurricane.hurricane.tcp.connection;

import com.hurricane.hurricane.common.TcpCallback;
import com.hurricane.hurricane.common.EventLoop;
import com.hurricane.hurricane.tcp.callback.TcpReadHandler;
import com.hurricane.hurricane.tcp.callback.TcpWriteHandler;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;


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
      closeConnection();
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
  public void closeConnection() {
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
  public boolean isConnectionClosed() {
    if (!socketChannel.isOpen()) {
      logger.warn("Closed channel = " + socketChannel);
      return true;
    }

    return false;
  }

  /**
   * Set Tcp Read handler for this Tcp connection. When new handler is set, we should check if this can be triggered
   * immediately, e.g., cache has enough data to process. If the handler cannot be run immediately, we need to make sure
   * this key register the READ event.
   * @param readHandler set new handler for this Tcp connection
   */
  public void setReadHandler(TcpReadHandler readHandler) {
    if (isConnectionClosed()) {
      return;
    }

    if (readHandler != null && readHandler.test(this)) {
      readHandler.run(this);
      this.readHandler = null;
    } else {
      this.readHandler = readHandler;
      this.key.interestOpsOr(SelectionKey.OP_READ);
    }
  }

  /**
   * Set Tcp Write handler for this Tcp connection. When new handler is set, we should check if this can be triggered
   * immediately. Also, we don't need to add interest in WRITE operation, prepareWriteData() is better place to do it.
   * @param writeHandler TCP write handler
   * @param data data that we want to send to the client
   */
  public void setWriteHandlerWithData(TcpWriteHandler writeHandler, @NotNull byte[] data) {
    if (isConnectionClosed()) {
      return;
    }

    // Write the data to the cache and register WRITE event
    key.interestOpsOr(SelectionKey.OP_WRITE);
    writeManager.writeDataToCache(data);
    if (writeManager.isCacheOverflow()) {
      logger.warn("Reached maximum read cache size, close channel = " + socketChannel);
      closeConnection();
    }

    // Set write handler and run it immediately if needed
    if (writeHandler != null && writeHandler.test(this)) {
      writeHandler.run(this);
      this.writeHandler = null;
    } else {
      this.writeHandler = writeHandler;
    }
  }

  /**
   * Check if the TCP connection still need to write data
   * @return if the TCP connection still need to write data
   */
  public boolean isWriting() {
    return !writeManager.isCacheEmpty();
  }

  /**
   * Clear the write handler
   */
  public void clearWriteHandler() {
    writeHandler = null;
  }

  /**
   * Clear the write handler
   */
  public void clearReadHandler() {
    readHandler = null;
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

  public SelectionKey getKey() {
    return key;
  }
}

package com.hurricane.hurricane.tcp.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 * This class processes client socket WRITE event and maintains read cache.
 */
public class TcpWriteManager {
  private final static Logger logger = Logger.getLogger(TcpWriteManager.class);

  /**
   * ByteBuffer used to read data from client socket channel, then store it in cache.
   */
  private ByteBuffer writeByteBuffer;

  /**
   * In NIO we don't know how much data we can write in a single write event, therefore caller can write data to this
   * cache, and this manager will write data when feasible until the cache is empty.
   */
  private Deque<Byte> writeCache;

  /**
   * Max size for write cache
   */
  private int maxCacheSize;

  public TcpWriteManager() {
    this(TcpConnection.DEFAULT_MAX_CACHE_SIZE, TcpConnection.DEFAULT_BYTE_BUFFER_SIZE);
  }

  public TcpWriteManager(int maxCacheSize, int byteBufferSize) {
    this.writeByteBuffer = ByteBuffer.allocate(byteBufferSize);
    this.maxCacheSize = maxCacheSize;
    this.writeCache = new ArrayDeque<>();
  }

  /**
   * Handle a WRITE IO event.
   *  1. Retrieve data from write cache if byteBuffer is empty
   *  2. Write data to client socket
   *  3. Trigger callback if needed
   * @throws IOException Some IO errors happen in writing data to channel
   * @param tcpConnection tcp connection
   */
  protected void handleWriteEvent(TcpConnection tcpConnection) throws IOException {
    // If there is no data in byteBuffer or cache, we should stop handling the write event
    var isEmptyByteBuffer = writeByteBuffer.position() == 0;
    if (isEmptyByteBuffer && writeCache.isEmpty()) {
      logger.info("Current key = " + tcpConnection.key + " has no data to write, remove interest of WRITE");
      tcpConnection.key.interestOpsAnd(~SelectionKey.OP_WRITE);
      return;
    }

    // Transfer as much data as possible from cache to byteBuffer
    while (writeByteBuffer.hasRemaining() && !writeCache.isEmpty()) {
      writeByteBuffer.put(writeCache.poll());
    }

    // Write data in byteBuffer to client channel
    writeByteBuffer.flip();
    try {
      tcpConnection.socketChannel.write(writeByteBuffer);
    } catch (IOException e) {
      logger.warn("Failed to write data to client channel = " + tcpConnection.socketChannel, e);
      tcpConnection.closeTcpConnection();
      throw e;
    }

    // Compact this buffer in case of partial write and makes the byteBuffer ready for reading data from cache
    writeByteBuffer.compact();

    // Trigger callback if needed
    var writeCallback = tcpConnection.getWriteHandler();
    if (writeCallback != null && writeCallback.test(tcpConnection)) {
      writeCallback.run(tcpConnection);
      tcpConnection.setWriteHandler(null);
    }
  }

  /**
   * Write the given data to write cache
   * @param data data that we want to write to client
   */
  protected void writeDataToCache(byte[] data) {
    for (var byteData : data) {
      this.writeCache.offer(byteData);
    }
  }

  /**
   * Test if the write cache is overflowed
   * @return if the write cache is overflowed
   */
  protected boolean isCacheOverflow() {
    return writeCache.size() > maxCacheSize;
  }

  /**
   * Test if the write cache is empty
   * @return if the write cache is empty or not
   */
  public boolean isCacheEmpty() {
    return writeCache.isEmpty();
  }
}

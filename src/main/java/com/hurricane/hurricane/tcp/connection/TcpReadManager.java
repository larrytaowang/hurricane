package com.hurricane.hurricane.tcp.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * This class processes client socket READ event and maintains read cache.
 */
public class TcpReadManager {
  private final static Logger logger = Logger.getLogger(TcpReadManager.class);

  /**
   * ByteBuffer used to read data from client socket channel, then store it in cache.
   */
  private ByteBuffer readByteBuffer;

  /**
   * In NIO we don't know how much data we can read in a single read event. All the read data will be stored in this
   * cache.
   */
  private LinkedList<Byte> readCache;

  /**
   * Max size for read cache
   */
  private int maxCacheSize;

  public TcpReadManager() {
    this(TcpConnection.DEFAULT_MAX_CACHE_SIZE, TcpConnection.DEFAULT_BYTE_BUFFER_SIZE);
  }

  public TcpReadManager(int maxCacheSize, int byteBufferSize) {
    this.readByteBuffer = ByteBuffer.allocate(byteBufferSize);
    this.readCache = new LinkedList<>();
    this.maxCacheSize = maxCacheSize;
  }

  /**
   * Handle a READ IO event.
   *  1. Read data from client channel
   *  2. Store the read data in the cache
   *  3. Trigger callback if needed
   * @throws IOException Some IO errors happen in reading data from channel
   * @param connection tcp connection
   */
  protected void handleReadEvent(TcpConnection connection) throws IOException {
    // read data from client channel
    long bytesRead;
    try {
      bytesRead = connection.socketChannel.read(readByteBuffer);
    } catch (IOException e) {
      logger.warn("Failed to read data from client channel = " + connection.socketChannel, e);
      connection.closeTcpConnection();
      throw e;
    }

    // If client channel has reached end-of-stream, close the channel and return
    if (bytesRead == -1) {
      connection.closeTcpConnection();
      return;
    }

    // If there is no data we can process, simply return
    if (bytesRead == 0) {
      return;
    }

    // Fetch data from client channel and store in the read cache.
    byte[] readBytes = new byte[readByteBuffer.position() + 1];
    readByteBuffer.rewind();
    readByteBuffer.get(readBytes);
    for (var readByte : readBytes) {
      readCache.add(readByte);
    }
    readByteBuffer.clear();
    logger.info("Receive data, count = " + bytesRead + " in channel = " + connection.socketChannel);

    // Check if the cache has overflowed
    if (readCache.size() > maxCacheSize) {
      logger.warn("Reached maximum read cache size, close channel = " + connection.socketChannel);
      connection.closeTcpConnection();
      return;
    }

    // Run the callback if needed
    if (connection.getReadHandler() != null && connection.getReadHandler().test(connection)) {
      connection.getReadHandler().run(connection);
      connection.setReadHandler(null);
    }
  }

  /**
   * Consume desired count of bytes in read cache.
   * @param bytesCount count of bytes to consume
   * @return oldest bytes in read cache with desired count
   */
  public Optional<byte[]> consume(int bytesCount) {
    if (bytesCount > readCache.size()) {
      logger.warn(
          "This should not happen. Try consume count = " + bytesCount + " bytes from cache while current cache size = "
              + readCache.size());
      return Optional.empty();
    }

    byte[] result = new byte[bytesCount];
    for (int i = 0; i < bytesCount; i++) {
      var digit = readCache.poll();
      if (digit != null) {
        result[i] = digit;
      } else {
        logger.warn("This should not happen. When consuming data from readCache, data[" + i + "] is null.");
      }
    }

    return Optional.of(result);
  }

  /**
   * Return read only view of read cache
   * @return read only view of read cache
   */
  public List<Byte> getUnmodifiableReadCache() {
    return Collections.unmodifiableList(readCache);
  }
}

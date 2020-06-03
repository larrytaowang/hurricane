package com.hurricane.hurricane;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * A utility class to write to and read from a non-blocking socket channel. We support three methods: write(),
 * readUntil(), and readBytes(). All of the methods take callbacks (since writing and reading are non-blocking.
 */
public class SocketHandler {
  private final static Logger logger = Logger.getLogger(SocketHandler.class);

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
  private SelectionKey key;

  /**
   * Socket Channel associated with the given key
   */
  private SocketChannel socketChannel;

  /**
   * A global eventLoop instance.
   */
  private EventLoop eventLoop;

  /**
   * Max size for readCache and writeCache
   */
  private int maxCacheSize;

  private ByteBuffer readByteBuffer;

  private ByteBuffer writeByteBuffer;

  /**
   * In NIO we don't know how much data we can read in a single read event. All the read data will be stored in this
   * cache for caller to read.
   */
  private Deque<Byte> readCache;

  /**
   * In NIO we don't know how much data we can write in a single write event, therefore caller can write data to this
   * cache, and handler will write all the data eventually.
   */
  private Deque<Byte> writeCache;

  /**
   * Used for readUntil() function. Call callback when we read the given delimiter. This handler stores bytes so
   * a delimiter provided by user will be decoded to bytes.
   */
  private List<Byte> delimiter;

  /**
   * Used for readBytes() function. Call callback when we read the given number of bytes.
   */
  private Integer readBytesCount;

  /**
   * Callback that will be run when required read data is collected
   */
  private Callback readCallback;

  /**
   * Callback that will be run when all the data finishes writing.
   */
  private Callback writeCallback;

  /**
   * Callback that will be run when the selection key is closed.
   */
  private Callback closeCallback;

  public SocketHandler(SelectionKey key) throws IOException {
    this(key, DEFAULT_MAX_CACHE_SIZE, DEFAULT_BYTE_BUFFER_SIZE);
  }

  public SocketHandler(SelectionKey key, int maxCacheSize, int byteBufferSize) throws IOException {
    this.key = key;
    this.socketChannel = (SocketChannel) key.channel();
    this.eventLoop = EventLoop.getInstance();
    this.maxCacheSize = maxCacheSize;
    this.readByteBuffer = ByteBuffer.allocate(byteBufferSize);
    this.writeByteBuffer = ByteBuffer.allocate(byteBufferSize);
    this.readCache = new ArrayDeque<>();
    this.writeCache = new ArrayDeque<>();
  }

  // TODO: If I want to seperate the responsibility of read_until and read_delimiter, can use two fields =>
  // readCallback, callbackPredicate + writeCallback, writeCallbackPredicate

  /**
   * Handle IO event in for this election key
   */
  public void handleEvent() throws IOException {
    if (!key.isValid()) {
      logger.warn("Got events for invalid key = " + key);
      closeSocketChannel();
      return;
    }

    // Handle Accept, Read, Write events
    if (key.isAcceptable()) {
      handleAccept();
    }

    if (key.isReadable()) {
      handleRead();
    }

    if (key.isValid() && key.isWritable()) {
      handleWrite();
    }

    // Update the interested Ops
    if (delimiter != null || readBytesCount != null) {
      key.interestOpsOr(SelectionKey.OP_READ);
    }

    if (!writeCache.isEmpty()) {
      key.interestOpsOr(SelectionKey.OP_WRITE);
    }
  }

  /**
   * Handle ACCEPT event for this selection key
   * @throws IOException If some I/O error occurs
   */
  private void handleAccept() throws IOException {
    var clientChannel = ((ServerSocketChannel) key.channel()).accept();
    clientChannel.configureBlocking(false);
    var clientKey = clientChannel.register(key.selector(), SelectionKey.OP_READ);
    eventLoop.addHandler(clientKey, new SocketHandler(clientKey));
  }

  @SuppressWarnings("unchecked")
  private void handleRead() throws IOException {
    // read data from client channel
    long bytesRead;
    try {
      bytesRead = socketChannel.read(readByteBuffer);
    } catch (IOException e) {
      logger.warn("Failed to read data from client channel = " + socketChannel, e);
      closeSocketChannel();
      throw e;
    }

    // client channel has reached end-of-stream
    if (bytesRead == -1) {
      closeSocketChannel();
      return;
    } else if (bytesRead == 0) {
      return;
    }

    // Fetch data from client channel
    byte[] readBytes = new byte[readByteBuffer.remaining()];
    readByteBuffer.get(readBytes);
    for (var readByte : readBytes) {
      readCache.add(readByte);
    }
    readByteBuffer.clear();

    if (readCache.size() > maxCacheSize) {
      logger.warn("Reached maximum read cache size, close channel = " + socketChannel);
      closeSocketChannel();
      return;
    }

    if (readCallback != null && readCache.size() >= readBytesCount) {
      // Apply handler if we have read required bytes
      readCallback.run(null);
      readBytesCount = null;
      readCallback = null;
    } else if (delimiter != null) {
      // Apply handler if we have delimiter
      var index = Collections.indexOfSubList((List<Byte>) readCache, delimiter);
      if (index != -1) {
        readCallback.run(null);
        delimiter = null;
        readCallback = null;
      }
    }
  }

  private void handleWrite() throws IOException {
    if (!writeByteBuffer.hasRemaining()) {
      writeByteBuffer.clear();

      // Write as much data as possible to byteBuffer
      while (writeByteBuffer.hasRemaining() && !writeCache.isEmpty()) {
        writeByteBuffer.put(writeCache.poll());
      }

      writeByteBuffer.flip();
    }

    try {
      socketChannel.write(writeByteBuffer);
    } catch (IOException e) {
      logger.warn("Failed to write data to client channel = " + socketChannel, e);
      closeSocketChannel();
      throw e;
    }

    if (writeCache.isEmpty() && writeCallback != null) {
      writeCallback.run(new Object[0]);
      writeCallback = null;
    }
  }

  /**
   * When we read the given delimiter, call the callback with the data before delimiter
   * @param delimiter call the callback when we see this delimiter
   * @param callback the callback we would run when we see delimiter
   */
  @SuppressWarnings("unchecked")
  public void readUntil(List<Byte> delimiter, Callback callback) throws Exception {
    int delimiterIndex = Collections.indexOfSubList((List<Byte>) readCache, delimiter);

    // When we call readUntil(), we may have read data from channel, so it is possible that we already have the
    // available data for callback.
    if (delimiterIndex != -1) {
      var consumedBytesCount = delimiterIndex + delimiter.size();
      var arguments = new Object[]{consume(consumedBytesCount)};
      callback.run(arguments);
      return;
    }

    // If we have not seen delimiter, save the delimiter and callback, then register READ event. Afterwards, whenever
    // new data is read from channel, handleRead() would be called, which checks if we have the data for delimiter or
    // not. If yes, run the callback with the data before delimiter.
    this.delimiter = delimiter;
    this.readCallback = callback;
    this.key.interestOpsOr(SelectionKey.OP_READ);
  }

  /**
   * Call callback when we read the given number of bytes
   * @param numBytes run callback when we read this number of bytes
   * @param callback the callback to run when we read the given number of bytes
   */
  public boolean readBytes(int numBytes, Callback callback) throws Exception {
    // When we call readBytes(), we may have read enough bytes from channel, so we can run the callback directly
    if (readCache.size() >= numBytes) {
      var arguments = new Object[]{consume(numBytes)};
      callback.run(arguments);
      return true;
    }

    if (checkClosed()) {
      return false;
    }

    // If we have not read enough bytes, save the required number of bytes and callback, then register READ event.
    // Afterwards, whenever new data is read from channel, handleRead() would be called, which checks if we have enough
    // bytes or not. If yes, run the callback with the data before delimiter.
    this.readBytesCount = numBytes;
    this.readCallback = callback;
    this.key.interestOpsOr(SelectionKey.OP_READ);
    return true;
  }

  /**
   * Write the given data to this stream.
   *
   * If callback is given, we call it when all of the buffered write
   * data has been successfully written to the stream. If there was
   * previously buffered write data and an old write callback, that
   * callback is simply overwritten with this new callback.
   * @param data Write this data to stream
   * @param callback Callback to run when all the given data has been written to the channel
   */
  public boolean write(byte[] data, Callback callback) {
    if (checkClosed()) {
      return false;
    }

    for (var byteData : data) {
      this.writeCache.offer(byteData);
    }
    this.key.interestOpsOr(SelectionKey.OP_WRITE);
    this.writeCallback = callback;
    return true;
  }

  private byte[] consume(int length) throws Exception {
    byte[] result = new byte[length];
    for (int i = 0; i < length; i++) {
      var digit = readCache.poll();
      if (digit == null) {
        throw new Exception(
            "Want to consumer length = " + length + " bytes, however, the read cache does not have " + "enough data");
      } else {
        result[i] = digit;
      }
    }

    return result;
  }

  private void closeSocketChannel() {
    eventLoop.removeHandler(key);
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

  private boolean checkClosed() {
    if (!socketChannel.isOpen()) {
      logger.warn("Closed channel = " + socketChannel);
      return true;
    }

    return false;
  }

  public void setCloseCallback(Callback closeCallback) {
    this.closeCallback = closeCallback;
  }
}

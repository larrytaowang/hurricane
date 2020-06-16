package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * When we have the given delimiter in the read cache, trigger the TcpPostEventCallback with the data before
 * delimiter (delimiter bytes are also included).
 */
public class TcpReadDelimiterHandler extends TcpReadHandler {
  private final static Logger logger = Logger.getLogger(TcpReadDelimiterHandler.class);

  /**
   * List of bytes of delimiter
   */
  private List<Byte> delimiter;

  /**
   * Position of delimiter in the write cache. If not exists, use -1
   */
  private int delimiterIndex;

  public TcpReadDelimiterHandler(String delimiter, TcpReadCallback tcpCallback) {
    this(delimiter.getBytes(StandardCharsets.UTF_8), tcpCallback);
  }

  public TcpReadDelimiterHandler(byte[] delimiter, TcpReadCallback tcpCallback) {
    super(tcpCallback);
    this.delimiterIndex = -1;

    // Convert byte[] to List<Byte>. byte[] is caller friendly, while List<Byte> is handy for indexing sub list.
    this.delimiter = new ArrayList<>();
    for (var singleByte : delimiter) {
      this.delimiter.add(singleByte);
    }
  }

  /**
   * Consume all the bytes before delimiter (delimiter bytes are also included), then wrap them as an argument, pass
   * to the Tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   */
  @Override
  public void run(TcpConnection tcpConnection) {
    var bytesCount = delimiterIndex + delimiter.size();
    logger.info("Run TcpReadDelimiterCallback, consumed bytesCount = " + bytesCount);
    tcpConnection.getReadManager().consume(bytesCount).ifPresent(x -> tcpCallback.run(tcpConnection, x));
  }

  /**
   * Test if the read cache has delimiter or not. If yes, we can run the Tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   * @return if Tcp callback should be triggered
   */
  @Override
  public boolean test(TcpConnection tcpConnection) {
    var readCache = tcpConnection.getReadManager().getUnmodifiableReadCache();
    delimiterIndex = Collections.indexOfSubList(readCache, delimiter);
    return delimiterIndex != -1;
  }
}

package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * When we receive desired count of bytes in the read cache, trigger the Tcp callback with these bytes.
 */
public class TcpReadBytesHandler extends TcpReadHandler {
  private final static Logger logger = Logger.getLogger(TcpReadBytesHandler.class);

  /**
   * Tcp callback is triggered when read cache has at least this amount of bytes
   */
  private int requitedBytesCount;

  public TcpReadBytesHandler(int requitedBytesCount, TcpReadCallback callback) {
    super(callback);
    this.requitedBytesCount = requitedBytesCount;
  }

  /**
   * Consume bytes of desired count, wrap them as an argument, the trigger Tcp callback.
   * @param connection TCP connection that this callback hosts
   */
  @Override
  public void run(TcpConnection connection) {
    logger.info("Run TcpReadBytesCallback, consumed bytesCount = " + requitedBytesCount);
    connection.getReadManager()
        .consume(requitedBytesCount)
        .ifPresent(x -> tcpCallback.run(connection, x));
  }

  /**
   * Test if the read cache has enough bytes or not. If yes, we can run the Tcp callback.
   * @param connection TCP connection that this callback hosts
   * @return if Tcp callback should be triggered
   */
  @Override
  public boolean test(TcpConnection connection) {
    return connection.getReadManager().getUnmodifiableReadCache().size() >= requitedBytesCount;
  }
}

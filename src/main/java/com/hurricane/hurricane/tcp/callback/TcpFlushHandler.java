package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * When all the data in the write cache has been flused, trigger the tcp callback.
 */
public class TcpFlushHandler extends TcpWriteHandler {
  private final static Logger logger = Logger.getLogger(TcpFlushHandler.class);

  public TcpFlushHandler(TcpWriteCallback callback) {
    super(callback);
  }

  /**
   * Trigger Tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   */
  @Override
  public void run(TcpConnection tcpConnection) {
    logger.info("All the data in write cache has been flushed, run Tcp callback");
    callback.run(tcpConnection);
  }

  /**
   * Check if data in write cache has been flushed. If yes we can call the run() method to trigger the tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   * @return if Tcp callback should be triggered.
   */
  @Override
  public boolean test(TcpConnection tcpConnection) {
    return tcpConnection.getWriteManager().isCacheEmpty();
  }
}

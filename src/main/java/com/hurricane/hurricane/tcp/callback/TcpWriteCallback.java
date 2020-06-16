package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;


/**
 * @author larrytaowang
 *
 * After a socket WRITE IO event is processed, we may want to run this TCP callback if needed, e.g., all the data in
 * write cache has been flushed.
 */
public interface TcpWriteCallback {
  /**
   * Run the Tcp callback with the TCP connection and arguments.
   * @param connection TCP connection that this callback hosts
   */
  void run(TcpConnection connection);
}

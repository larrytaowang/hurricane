package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;


/**
 * @author larrytaowang
 *
 * After a socket READ IO event is processed, we may want to run this TCP callback if needed. Below are some examples:
 *
 * <ul>
 *  <li> Run a Tcp Callback after we received more than 'n' bytes
 *  <li> Run a Tcp Callback after we received a delimiter
 * </ul>
 */
public interface TcpReadCallback {
  /**
   * Run the Tcp callback with the TCP connection and arguments.
   * @param connection TCP connection that this callback hosts
   * @param bytes arguments for this callback
   */
  void run(TcpConnection connection, byte[] bytes);
}

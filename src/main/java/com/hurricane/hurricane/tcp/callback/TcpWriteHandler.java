package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;


/**
 * @author larrytaowang
 *
 * After a TcpWriteManager handles WRITE IO event, it will run Tcp callback if needed (test() returns true).
 */
public abstract class TcpWriteHandler {
  /**
   * When condition is met in test(), this Tcp callback should be triggered in run() method.
   */
  protected TcpCallback callback;

  protected TcpWriteHandler(TcpCallback callback) {
    this.callback = callback;
  }

  /**
   * Check if desired condition has been met. If yes, we can call the run() method to trigger the callback.
   * @param tcpConnection TCP connection that this callback hosts
   * @return if we can trigger run() method or not.
   */
  abstract public boolean test(TcpConnection tcpConnection);

  /**
   * Trigger the callback.
   * @param tcpConnection TCP connection that this callback hosts
   */
  abstract public void run(TcpConnection tcpConnection);
}

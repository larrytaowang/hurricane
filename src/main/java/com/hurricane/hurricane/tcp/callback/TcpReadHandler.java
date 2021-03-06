package com.hurricane.hurricane.tcp.callback;

import com.hurricane.hurricane.tcp.connection.TcpConnection;


/**
 * @author larrytaowang
 *
 * After a TcpReadManager handles READ IO event, it will run TcpCallback if needed (test() returns true).
 */
public abstract class TcpReadHandler {
  /**
   * When condition is met in test(), this callback should be triggered in run() method.
   */
  protected TcpReadCallback tcpCallback;

  protected TcpReadHandler(TcpReadCallback tcpCallback) {
    this.tcpCallback = tcpCallback;
  }

  /**
   * Check if desired condition has been met. If yes, we can call the run() method to trigger the Tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   * @return if we can trigger run() method or not.
   */
  abstract public boolean test(TcpConnection tcpConnection);

  /**
   * Trigger the Tcp callback.
   * @param tcpConnection TCP connection that this callback hosts
   */
  abstract public void run(TcpConnection tcpConnection);
}

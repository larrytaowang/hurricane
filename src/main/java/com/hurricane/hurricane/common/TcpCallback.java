package com.hurricane.hurricane.common;

/**
 * @author larrytaowang
 *
 * A callback can be
 * <ul>
 *  <li> added to callbacks set directly in Event Loop
 *  <li> associated to a time event then added to Event Loop
 * </ul>
 */
public interface TcpCallback {
  /**
   * Run the callback
   * @param args arguments of this callback
   */
  void run(Object[] args);
}

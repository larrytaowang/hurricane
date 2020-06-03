package com.hurricane.hurricane;

import jdk.jshell.spi.ExecutionControl;


/**
 * @author larrytaowang
 *
 * A callback can be
 * <ul>
 *  <li> added to callbacks set in Event Loop
 *  <li> associated to a time event then added to Event Loop
 *  <li> associated to a IO event in Event Loop
 * </ul>
 */
public interface Callback {
  /**
   * Run the callback.
   */
   void run(Object[] args);
}

package com.hurricane.hurricane;

import java.nio.channels.SelectionKey;


/**
 * @author larrytaowang
 */
public interface Handler {
  void run(SelectionKey key);
}

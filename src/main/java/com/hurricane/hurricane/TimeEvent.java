package com.hurricane.hurricane;

import org.jetbrains.annotations.NotNull;


/**
 * @author larrytaowang
 *
 * A time event, which should be executed at the deadline in IOLoop iteration.
 */
public class TimeEvent {
  /**
   * The unix time stamp in milliseconds, at which the callback should be executed at the IO Loop.
   */
  private long deadline;

  /**
   * The callback to be executed when timeout.
   */
  private Callback callback;

  public TimeEvent(long deadline, @NotNull Callback callback) {
    this.deadline = deadline;
    this.callback = callback;
  }

  public long getDeadline() {
    return deadline;
  }

  @NotNull
  public Callback getCallback() {
    return callback;
  }
}

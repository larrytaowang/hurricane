package com.hurricane.hurricane.common;

import org.jetbrains.annotations.NotNull;


/**
 * @author larrytaowang
 *
 * A time event, which should be executed at the deadline in EventLoop iteration.
 */
public class TimeEvent {
  /**
   * The unix time stamp in milliseconds, at which the callback should be executed at the event Loop.
   */
  private long deadline;

  /**
   * The callback to be executed when overdue.
   */
  private TcpCallback callback;

  public TimeEvent(long deadline, @NotNull TcpCallback callback) {
    this.deadline = deadline;
    this.callback = callback;
  }

  public long getDeadline() {
    return deadline;
  }

  @NotNull
  public TcpCallback getCallback() {
    return callback;
  }
}

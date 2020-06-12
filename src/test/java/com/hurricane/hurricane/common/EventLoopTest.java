package com.hurricane.hurricane.common;

import com.hurricane.hurricane.tcp.TcpServer;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Test if adding/removing callbacks and time events works. Network IO is covered in TcpConnectionTest.
 */
public class EventLoopTest {
  /**
   * If EventLoop.start() returns within this time range, then we say it returns immediately.
   */
  public static final long TIME_THRESHOLD = EventLoop.DEFAULT_SELECT_TIMEOUT / 10;

  /**
   * A global EventLoop instance
   */
  private EventLoop eventLoop;

  /**
   * Start time of the IO Loop
   */
  private long startTime;

  /**
   * A variable to count of this callable being called.
   */
  private int calledCount;

  @Before
  public void setUp() throws Exception {
    TcpServer.init(null);
    this.eventLoop = EventLoop.getInstance();
    this.calledCount = 0;
    this.startTime = System.currentTimeMillis();
  }

  @Test
  public void stop() {
    eventLoop.stop();
    eventLoop.start();
    Assert.assertTrue(returnImmediately());
  }

  @Test
  public void addTimeEvent() {
    // Add time event to the loop. These events expire at the start time so they should be executed at first iteration.
    var addCount = 10;
    for (int i = 0; i < addCount; i++) {
      var timeEvent = new TimeEvent(startTime, createCallback());
      eventLoop.addTimeEvent(timeEvent);
    }

    eventLoop.start();
    Assert.assertEquals(addCount, calledCount);
    Assert.assertTrue(returnImmediately());
  }

  @Test
  public void removeTimeEvent() {
    List<TimeEvent> timeEvents = new LinkedList<>();

    // Add callbacks to the event loop
    var addCount = 10;
    for (int i = 0; i < addCount; i++) {
      var timeEvent = new TimeEvent(startTime, createCallback());
      eventLoop.addTimeEvent(timeEvent);
      timeEvents.add(timeEvent);
    }

    // Remove callbacks from the event loop
    int removeCount = 5;
    for (int i = 0; i < removeCount; i++) {
      eventLoop.removeTimeEvent(timeEvents.get(i));
    }

    eventLoop.start();
    Assert.assertEquals(addCount - removeCount, calledCount);
    Assert.assertTrue(returnImmediately());
  }

  @Test
  public void addCallback() {
    // Add callbacks to the event loop
    var addCount = 10;
    for (int i = 0; i < addCount; i++) {
      eventLoop.addCallback(createCallback());
    }

    eventLoop.start();
    Assert.assertEquals(addCount, calledCount);
    Assert.assertTrue(returnImmediately());
  }

  @Test
  public void removeCallback() {
    List<TcpCallback> callbacks = new LinkedList<>();

    // Add callbacks to the event loop
    var addCount = 10;
    for (int i = 0; i < addCount; i++) {
      var callback = createCallback();
      eventLoop.addCallback(callback);
      callbacks.add(callback);
    }

    // Remove callbacks from the event loop
    int removeCount = 5;
    for (int i = 0; i < removeCount; i++) {
      eventLoop.removeCallback(callbacks.get(i));
    }

    eventLoop.start();
    Assert.assertEquals(addCount - removeCount, calledCount);
    Assert.assertTrue(returnImmediately());
  }

  /**
   * Use this function to determine if the eventLoop.start() returns immediately. If the function returns in a given
   * time threshold, we say it returns 'immediately'. Also, we are very tolerate with the threshold to avoid flaky.
   * @return if the eventLoop.start() returns immediately
   */
  private boolean returnImmediately() {
    return System.currentTimeMillis() - startTime < TIME_THRESHOLD;
  }

  /**
   * Created a simple callback for testing.
   * @return a callback for testing.
   */
  private TcpCallback createCallback() {
    return args -> {
      calledCount += 1;
      eventLoop.stop();
    };
  }
}
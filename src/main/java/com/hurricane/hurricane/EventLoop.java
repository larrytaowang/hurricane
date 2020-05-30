package com.hurricane.hurricane;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * A forever loop that handles callbacks, time events and IO events. Most single-threaded applications have a single,
 * global IOLoop instance.
 */
public class EventLoop {
  private final static Logger logger = Logger.getLogger(EventLoop.class);

  /**
   * Registered callbacks. Each callback will be executed in the current IO loop iteration.
   */
  private Set<Callback> callbacks;

  /**
   * Each selection key has an associated handler, which will be executed when the corresponding IO event is available.
   */
  private Map<SelectionKey, Handler> handlers;

  /**
   * Registered time events. Each time event's callback should be executed at the given deadline in IO Loop iteration.
   */
  private PriorityQueue<TimeEvent> timeEvents;

  /**
   * A selector for all network IO events
   */
  private Selector selector;

  /**
   * Flag for whether IOLoop is running.
   */
  private Boolean isRunning;

  /**
   * This is the flag for whether IO loop should continue or not. If we want to stop the IO loop after current
   * iteration, set this flag to true in one of the IO handlers via stop().
   */
  private Boolean isStopped;

  /**
   * Default select timeout in milliseconds
   */
  public static final long DEFAULT_SELECT_TIMEOUT = 3000;

  /**
   * A singleton IOLoop instance
   */
  private static EventLoop instance = null;

  /**
   * Constructor for Event Loop
   * @throws IOException If the program fails to open a selector, there is nothing we can do to recover, so we should
   * let it fail.
   */
  private EventLoop() throws IOException {
    this.selector = Selector.open();

    this.handlers = new HashMap<>();
    this.callbacks = new HashSet<>();
    this.timeEvents = new PriorityQueue<>(Comparator.comparing(TimeEvent::getDeadline));
    this.isRunning = false;
    this.isStopped = false;
  }

  /**
   * Most single-threaded applications have a single, global IOLoop. Use this method instead of passing around IOLoop
   * instances throughout the code.
   * @return a global IOLoop instance
   * @throws IOException failed to create an IOLoop instance because of selector open IO exception
   */
  public static EventLoop getInstance() throws IOException {
    if (instance == null) {
      instance = new EventLoop();
    }

    return instance;
  }

  /**
   * Registers the given handler to receive the given NIO events for this SelectionKey
   * @param key A key we want to register a caller for interested ops
   * @param handler handler that we want to associate with the given key
   * @param ops interested ops of the key
   */
  public void addHandler(SelectionKey key, Handler handler, int ops) {
    handlers.put(key, handler);
    key.interestOps(ops);
  }

  /**
   * Stop listening events for this SelectionKey. Also, remove the associated handler.
   * @param key We no longer want to receive events of this key's channel
   */
  public void removeHandler(SelectionKey key) {
    handlers.remove(key);
    key.cancel();
  }

  /**
   * Start the IO Loop. The loop will run until one of the IO handler calls stop(), which will make the loop stop after
   * the current event iteration completes.
   */
  public void start() {
    if (isStopped) {
      isStopped = false;
      return;
    }

    isRunning = true;
    while (true) {
      var selectTimeout = DEFAULT_SELECT_TIMEOUT;
      selectTimeout = handleCurrentCallbacks(selectTimeout);
      selectTimeout = handleTimeoutEvents(selectTimeout);

      if (!isRunning) {
        break;
      }

      handleIOEvents(selectTimeout);
    }

    // reset the stop flag so another start/stop pair can be issued
    isStopped = false;
  }

  /**
   * Select ready keys in given time out. If there in any ready key, execute the associated handler.
   * @param selectTimeout time out value used for select method
   */
  private void handleIOEvents(long selectTimeout) {
    try {
      int readyKeysCount = selector.select(selectTimeout);
      if (readyKeysCount == 0) {
        return;
      }
    } catch (IOException e) {
      logger.warn("Selector failed to select with exception, time out = " + selectTimeout, e);
    }

    // Get iterator on set of keys with IO to process
    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
    while (keyIterator.hasNext()) {
      SelectionKey key = keyIterator.next();

      // Execute handler associated with the IO event
      handlers.get(key).run(key);

      // Remove from set of selected keys
      keyIterator.remove();
    }
  }

  /**
   * Execute the callbacks of all the overtime time events. Also, adjust the time out value for next select operation
   * @param selectTimeout current time out value for next select operation
   * @return new time out value for next select operation
   */
  private long handleTimeoutEvents(long selectTimeout) {
    var newSelectTimeout = selectTimeout;
    if (!timeEvents.isEmpty()) {
      var now = System.currentTimeMillis();

      // timeEvents is a min-heap, so the root element has the earliest deadline. Keep polling and executing all the
      // overtime time events.
      while (!timeEvents.isEmpty() && timeEvents.peek().getDeadline() <= now) {
        var timeoutEvent = timeEvents.poll();
        if (timeoutEvent != null) {
          timeoutEvent.getCallback().run();
        }
      }

      // If there is any pending time event, the select operation should return at the earliest deadline.
      if (!timeEvents.isEmpty()) {
        var minSelectTimeout = timeEvents.peek().getDeadline() - now;
        newSelectTimeout = Math.min(newSelectTimeout, minSelectTimeout);
      }
    }

    return newSelectTimeout;
  }

  /**
   * Consume the current callbacks list and adjust and the time out value for next select operation.
   *
   * If we consume the callbacks list directly and new callbacks are added continuously, then IO event starvation may
   * happen. In order to prevent starvation, we should consume the copy of the callbacks list. Therefore the new
   * callbacks will be executed in the next iteration of the event loop.
   *
   * After a callback is executed, we should remove it from the callbacks set. If the final set is not empty, we should
   * not wait in next select before we run them.
   * @param selectTimeout current time out value for selector
   * @return new time out value for selector
   */
  private long handleCurrentCallbacks(long selectTimeout) {
    var callbacksCopy = new HashSet<>(callbacks);
    for (var callback : callbacksCopy) {
      if (callbacks.contains(callback)) {
        callbacks.remove(callback);
        callback.run();
      }
    }

    // If there is any new callback, we don't want to wait in select before we run them.
    return !callbacks.isEmpty() ? 0 : selectTimeout;
  }

  /**
   * Stop the loop after the current event loop iteration is complete. If the event loop is not currently running, the
   * next call to start() will return immediately.
   */
  public void stop() {
    isRunning = false;
    isStopped = true;
    wakeup();
  }

  /**
   * Returns true if this IOLoop is currently running
   * @return if the current IOLoop is running
   */
  public Boolean isRunning() {
    return isRunning;
  }

  /**
   * Add a new time event, whose callback should be executed at the deadline.
   * @param timeEvent the time event we want to add
   */
  public void addTimeEvent(TimeEvent timeEvent) {
    timeEvents.add(timeEvent);
  }

  /**
   * Remove the time event
   * @param event the timeout event we want to remove
   */
  public void removeTimeEvent(TimeEvent event) {
    timeEvents.remove(event);
  }

  /**
   * Add a new callback, which will be executed on the next IO loop iteration, then wake up the selector.
   * @param callback callback we want to trigger in the next IO loop
   */
  public void addCallback(Callback callback) {
    callbacks.add(callback);
    wakeup();
  }

  /**
   * Remove the given callback
   * @param callback callback we want to remove
   */
  public void removeCallback(Callback callback) {
    callbacks.remove(callback);
  }

  /**
   * Causes the current selection operation that has not yet returned to return immediately. If there is no selection
   * operation is currently in progress then the next invocation of a selection operation will return immediately.
   */
  private void wakeup() {
    selector.wakeup();
  }
}

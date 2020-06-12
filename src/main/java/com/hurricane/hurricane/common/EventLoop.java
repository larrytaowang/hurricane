package com.hurricane.hurricane.common;

import com.hurricane.hurricane.tcp.TcpServer;
import com.hurricane.hurricane.tcp.connection.TcpConnection;
import java.io.IOException;
import java.nio.channels.SelectionKey;
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
 * A forever loop that handles callbacks, time events and socket IO events. Most single-threaded applications have a
 * single, global IOLoop instance.
 */
public class EventLoop {
  private final static Logger logger = Logger.getLogger(EventLoop.class);

  /**
   * Registered callbacks. Each callback will be executed in the current IO loop iteration.
   */
  private Set<TcpCallback> callbacks;

  /**
   * This map contains selection key of each client socket, and its TcpConnection. A connection contains cache and
   * handlers for read/write socket IO events.
   */
  private Map<SelectionKey, TcpConnection> clientConnections;

  /**
   * Registered time events. Each time event's callback should be executed at the given deadline in event Loop iteration
   */
  private PriorityQueue<TimeEvent> timeEvents;

  /**
   * Flag for whether event loop is running.
   */
  private Boolean isRunning;

  /**
   * This is the flag for whether event loop should continue or not. If we want to stop the IO loop after current
   * iteration, set this flag to true via stop() in Callback, TimeEvent, or TcpCallback.
   */
  private Boolean isStopped;

  /**
   * Default select timeout in milliseconds
   */
  public static final long DEFAULT_SELECT_TIMEOUT = 3000;

  /**
   * A singleton EventLoop instance
   */
  private static EventLoop instance = null;

  /**
   * Constructor for Event Loop
   */
  private EventLoop() {
    this.clientConnections = new HashMap<>();
    this.callbacks = new HashSet<>();
    this.timeEvents = new PriorityQueue<>(Comparator.comparing(TimeEvent::getDeadline));
    this.isRunning = false;
    this.isStopped = false;
  }

  /**
   * Most single-threaded applications have a single, global EventLoop. Use this method instead of passing around
   * EventLoop instances throughout the code.
   * @return a global EventLoop instance
   */
  public static EventLoop getInstance() {
    if (instance == null) {
      instance = new EventLoop();
    }

    return instance;
  }

  /**
   * Registers the given TcpConnection to handle the given NIO events for this SelectionKey
   * @param key A key we want to register a TcpConnection for interested ops
   * @param tcpConnection connection that we want to associate with the given key, to handle read, write events
   */
  public void registerTcpConnection(SelectionKey key, TcpConnection tcpConnection) {
    clientConnections.put(key, tcpConnection);
  }

  /**
   * Stop listening socket IO events for this SelectionKey. Also, remove the associated TcpConnection.
   * @param key We no longer want to receive events of this key's channel
   */
  public void deregisterTcpConnection(SelectionKey key) {
    clientConnections.remove(key);
    key.cancel();
  }

  /**
   * Start the EventLoop. The loop will run until stop() is called, which will make the loop stop after the current
   * event iteration completes.
   */
  public void start() {
    if (isStopped) {
      isStopped = false;
      return;
    }

    // A forever loop that will execute callbacks, time events, socket IO events accordingly.
    isRunning = true;
    while (true) {
      var selectTimeout = DEFAULT_SELECT_TIMEOUT;
      selectTimeout = handleCurrentCallbacks(selectTimeout);
      selectTimeout = handleTimeoutEvents(selectTimeout);

      if (!isRunning) {
        break;
      }

      handleSocketEvents(selectTimeout);
    }

    // reset the stop flag so another start/stop pair can be issued
    isStopped = false;
  }

  /**
   * Select ready keys in given time out and handle accept, read, write socket events accordingly.
   * @param selectTimeout time out value used for select method
   */
  private void handleSocketEvents(long selectTimeout) {
    try {
      int readyKeysCount = TcpServer.getSelector().select(selectTimeout);
      if (readyKeysCount == 0) {
        return;
      }
    } catch (IOException e) {
      logger.warn("Selector failed to select with exception, time out = " + selectTimeout, e);
    }

    // Get iterator on set of keys with IO to process
    Iterator<SelectionKey> keyIterator = TcpServer.getSelector().selectedKeys().iterator();
    while (keyIterator.hasNext()) {
      SelectionKey key = keyIterator.next();

      if (key.equals(TcpServer.getServerKey())) {
        TcpServer.handleServerSocketEvent();
      } else {
        handleOneClientSocketEvent(key);
      }

      keyIterator.remove();
    }
  }

  /**
   * A client selection key is ready, process the read, write socket IO events.
   * @param key Client Key that is ready for socket read, write events
   */
  private void handleOneClientSocketEvent(SelectionKey key) {
    TcpConnection tcpConnection = clientConnections.get(key);
    if (tcpConnection == null) {
      logger.warn("This should not happen. Skipped because no associated TcpConnection for Key = " + key);
    } else {
      try {
        tcpConnection.handleClientSocketEvent();
        logger.info("Successfully handled socket event for Key = " + key);
      } catch (IOException e) {
        logger.warn("Failed to handle socket event for Key = " + key, e);
      }
    }
  }

  /**
   * Execute the callbacks of all the overdue time events. Also, adjust the time out value for next select operation
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
        var timeEvent = timeEvents.poll();
        if (timeEvent != null) {
          logger.info("Execute overdue time event = " + timeEvent);
          timeEvent.getCallback().run(null);
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
        callback.run(null);
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
   * Add a new callback, which will be executed on the next event loop iteration, then wake up the selector.
   * @param callback callback we want to trigger in the next event loop
   */
  public void addCallback(TcpCallback callback) {
    callbacks.add(callback);
    wakeup();
  }

  /**
   * Remove the given callback
   * @param callback callback we want to remove
   */
  public void removeCallback(TcpCallback callback) {
    callbacks.remove(callback);
  }

  /**
   * Causes the current selection operation that has not yet returned to return immediately. If there is no selection
   * operation is currently in progress then the next invocation of a selection operation will return immediately.
   */
  private void wakeup() {
    TcpServer.getSelector().wakeup();
  }

  public Map<SelectionKey, TcpConnection> getClientConnections() {
    return clientConnections;
  }
}

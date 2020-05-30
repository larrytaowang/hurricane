package com.hurricane.hurricane;

import java.util.Optional;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;


/**
 * @author larrytaowang
 *
 * Our callback implementation is a simple wrapper for Callable with int return value. Subclasses may override
 * handleException() method to customize handler of exceptions.
 *
 * I am hesitating to add required return code in function run(), which seems like `C` style. However, I don't think
 * there an elegant way to run a method without parameter, without return value but can throw. "Runnable" has no return
 * value but cannot throw; "Supplier" returns something but cannot throw.
 *
 * Also, no need to override hashCode() and equals(), since we prefer identity semantics.
 */
public class Callback {
  private final static Logger logger = Logger.getLogger(Callback.class);

  /**
   * The returned error code of a callable run
   */
  private Integer returnCode;

  /**
   * Callable we would execute in a callback
   */
  private Callable<Integer> callable;

  public Callback(Callable<Integer> callable) {
    this.returnCode = 0;
    this.callable = callable;
  }

  /**
   * Run the callable. If there is any exception, call the handler.
   */
  public Optional<Integer> run() {
    try {
      returnCode = callable.call();
      return Optional.ofNullable(returnCode);
    } catch (Exception e) {
      handleException(e);
      return Optional.ofNullable(returnCode);
    }
  }

  /**
   * This method is called whenever a run failed and throws exception. By default, we simply logs the exception as an
   * error. Subclasses may override this method to customize reporting of exceptions.
   * @param e Exception thrown when run the callable
   */
  public void handleException(Exception e) {
    String errorMessage = "Failed to execute callback";
    if (returnCode != null) {
      errorMessage += " with return code = " + returnCode;
    }
    logger.warn(errorMessage + returnCode, e);
  }

  @Override
  public String toString() {
    return "Callback{" + "callable=" + callable + '}';
  }
}

package com.perunlabs.mokosh.running;

import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

/**
 * Represents parallel execution.
 *
 * @param <T>
 *          type of result of execution
 */
public interface Running<T> {
  /**
   * Blocks until execution is completed or aborted. Supplied result of execution might return
   * result or throw exception.
   *
   * @throws AbortException
   *           if waiting thread was interrupted
   */
  Supplier<T> await();

  /**
   * Tries to abort underlying execution.
   *
   * May not be successful if execution is already completed or it does not respond to interruption.
   * If execution was already aborted then nothing happens.
   */
  Running<T> abort();

  /**
   * @return true if execution is still running, false if it is completed or aborted
   */
  boolean isRunning();
}

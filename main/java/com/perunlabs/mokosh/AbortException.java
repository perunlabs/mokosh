package com.perunlabs.mokosh;

/**
 * Unchecked replacement for {@link InterruptedException}
 */
public class AbortException extends RuntimeException {
  public AbortException() {}

  public AbortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public AbortException(String message, Throwable cause) {
    super(message, cause);
  }

  public AbortException(String message) {
    super(message);
  }

  public AbortException(Throwable cause) {
    super(cause);
  }
}

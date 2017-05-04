package com.perunlabs.mokosh;

public class MokoshException extends RuntimeException {
  public MokoshException() {}

  public MokoshException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public MokoshException(String message, Throwable cause) {
    super(message, cause);
  }

  public MokoshException(String message) {
    super(message);
  }

  public MokoshException(Throwable cause) {
    super(cause);
  }

  public static void check(boolean condition) {
    if (!condition) {
      throw new MokoshException();
    }
  }
}

package com.perunlabs.mokosh.common;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedByInterruptException;

import com.perunlabs.mokosh.AbortException;

public class Unchecked {
  public static RuntimeException unchecked(IOException exception) {
    if (exception instanceof ClosedByInterruptException) {
      return new AbortException(exception);
    } else if (exception instanceof InterruptedIOException) {
      return new AbortException(exception);
    } else {
      return new UncheckedIOException(exception);
    }
  }

  public static Runnable unchecked(IoOperation operation) {
    return () -> {
      try {
        operation.run();
      } catch (IOException e) {
        throw unchecked(e);
      }
    };
  }

  public static interface IoOperation {
    void run() throws IOException;
  }
}

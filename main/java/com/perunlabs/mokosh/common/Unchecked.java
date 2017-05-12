package com.perunlabs.mokosh.common;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
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

  public static InputStream unchecked(InputStream input) {
    return new InputStream() {
      public int read() {
        try {
          return input.read();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public int read(byte[] bytes) {
        try {
          return input.read(bytes);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public int read(byte[] bytes, int offset, int length) {
        try {
          return input.read(bytes, offset, length);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public long skip(long count) {
        try {
          return input.skip(count);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public int available() {
        try {
          return input.available();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public void close() {
        try {
          input.close();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      @SuppressWarnings("sync-override")
      public void mark(int limit) {
        input.mark(limit);
      }

      @SuppressWarnings("sync-override")
      public void reset() {
        try {
          input.reset();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public boolean markSupported() {
        return input.markSupported();
      }

      public String toString() {
        return format("unchecked(%s)", input);
      }
    };
  }

  public static OutputStream unchecked(OutputStream output) {
    return new OutputStream() {
      public void write(int oneByte) {
        try {
          output.write(oneByte);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public void write(byte[] bytes) {
        try {
          output.write(bytes);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public void write(byte[] bytes, int offset, int length) {
        try {
          output.write(bytes, offset, length);
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public void flush() {
        try {
          output.flush();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public void close() {
        try {
          output.close();
        } catch (IOException e) {
          throw unchecked(e);
        }
      }

      public String toString() {
        return format("unchecked(%s)", output);
      }
    };
  }
}

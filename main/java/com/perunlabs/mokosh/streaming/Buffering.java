package com.perunlabs.mokosh.streaming;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Streams.pump;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Supplier;

import com.perunlabs.mokosh.running.Running;

public class Buffering extends Streaming {
  private final Running<Void> pumping;
  private final InputStream input;

  private Buffering(Running<Void> pumping, InputStream input) {
    this.pumping = pumping;
    this.input = input;
  }

  public static Streaming buffering(int size, InputStream input) {
    check(size > 0);
    check(input != null);
    try {
      PipedInputStream pipedInput = new PipedInputStream(size);
      PipedOutputStream pipedOutput = new PipedOutputStream(pipedInput);
      Running<Void> pumping = supplying(() -> {
        try {
          pump(input, pipedOutput);
          input.close();
          pipedOutput.close();
        } catch (IOException e) {
          throw unchecked(e);
        }
      });
      return new Buffering(pumping, pipedInput) {
        public String toString() {
          return format("buffering(%s, %s)", size, input);
        }
      };
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  public Supplier<Void> await() {
    return pumping.await();
  }

  public Running<Void> abort() {
    return pumping.abort();
  }

  public boolean isRunning() {
    return pumping.isRunning();
  }

  public int read() {
    try {
      return input.read();
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  public int read(byte[] b) {
    try {
      return input.read(b);
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  public int read(byte[] b, int off, int len) {
    try {
      return input.read(b, off, len);
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  public long skip(long n) {
    try {
      return input.skip(n);
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

  public synchronized void mark(int readlimit) {
    input.mark(readlimit);
  }

  public synchronized void reset() {
    try {
      input.reset();
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  public boolean markSupported() {
    return input.markSupported();
  }
}

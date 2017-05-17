package com.perunlabs.mokosh.streaming;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Streams.pump;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;
import static com.perunlabs.mokosh.running.Entangling.entangle;
import static com.perunlabs.mokosh.running.Supplying.supplying;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.running.Running;

public class Processing extends Streaming {
  private final Running<Void> pumping;
  private final Process process;
  private final InputStream input;

  private Processing(Running<Void> pumping, Process process) {
    this.pumping = pumping;
    this.process = process;
    this.input = process.getInputStream();
  }

  public static Streaming processing(Command command) {
    check(command != null);

    InputStream stdin = command.stdin.orElse(new ByteArrayInputStream(new byte[0]));
    OutputStream stderr = command.stderr.orElse(nullOutputStream());
    ProcessBuilder processBuilder = new ProcessBuilder(command.command);

    AtomicBoolean broken = new AtomicBoolean(false);
    Process process;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw unchecked(e);
    }

    Running<Void> pumpingStdin = supplying(unchecked(() -> {
      try (OutputStream processStdin = process.getOutputStream()) {
        try {
          pump(stdin, processStdin);
        } catch (RuntimeException | Error e) {
          broken.set(true);
          process.destroy();
        } finally {
          stdin.close();
        }
      }
    }));
    Running<Void> pumpingStderr = supplying(unchecked(() -> {
      try (InputStream processStderr = process.getErrorStream()) {
        pump(processStderr, stderr);
      } finally {
        if (!broken.get()) {
          stderr.close();
        }
      }
    }));
    Running<Void> pumping = entangle(pumpingStdin, pumpingStderr);
    return new Processing(pumping, process);
  }

  public boolean isRunning() {
    return process.isAlive() || pumping.isRunning();
  }

  public Supplier<Void> await() {
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
    pumping.await();
    int exitStatus = process.exitValue();
    return () -> {
      if (exitStatus != 0) {
        throw new RuntimeException("exit status = " + exitStatus);
      } else {
        return null;
      }
    };
  }

  public Running<Void> abort() {
    process.destroy();
    pumping.abort();
    return this;
  }

  public int read() throws IOException {
    return input.read();
  }

  public int read(byte[] b) throws IOException {
    return input.read(b);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    return input.read(b, off, len);
  }

  public long skip(long n) throws IOException {
    return input.skip(n);
  }

  public int available() throws IOException {
    return input.available();
  }

  public void close() throws IOException {
    input.close();
  }

  @SuppressWarnings("sync-override")
  public void mark(int readlimit) {
    input.mark(readlimit);
  }

  @SuppressWarnings("sync-override")
  public void reset() throws IOException {
    input.reset();
  }

  public boolean markSupported() {
    return input.markSupported();
  }

  private static OutputStream nullOutputStream() {
    return new OutputStream() {
      public void write(int b) {}

      public void write(byte[] b) {}

      public void write(byte[] b, int off, int len) {}
    };
  }
}

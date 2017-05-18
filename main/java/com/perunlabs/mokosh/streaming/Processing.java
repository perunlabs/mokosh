package com.perunlabs.mokosh.streaming;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Streams.pump;
import static com.perunlabs.mokosh.running.Entangling.entangle;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static java.lang.String.join;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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
    this.input = process.getErrorStream();
  }

  public static Streaming processing(Command command) {
    check(command != null);

    InputStream stdin = command.stdin.orElse(new ByteArrayInputStream(new byte[0]));
    OutputStream stderr = command.stderr.orElse(nullOutputStream());
    ProcessBuilder processBuilder = new ProcessBuilder(
        "/bin/sh",
        "-c",
        join(" ", command.command) + " 3>&1 1>&2 2>&3 3>&-");

    Process process;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    AtomicBoolean broken = new AtomicBoolean(false);

    Running<Void> pumpingStdin = supplying(() -> {
      try {
        OutputStream processStdin = process.getOutputStream();
        pump(stdin, processStdin);
        processStdin.close();
      } catch (RuntimeException | Error e) {
        broken.set(true);
        process.destroy();
        throw e;
      } catch (IOException e) {
        broken.set(true);
        process.destroy();
        throw new UncheckedIOException(e);
      }
      try {
        stdin.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
    Running<Void> pumpingStderr = supplying(() -> {
      try {
        InputStream processStderr = process.getInputStream();
        pump(processStderr, stderr);
        if (!broken.get()) {
          stderr.close();
        }
        processStderr.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
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

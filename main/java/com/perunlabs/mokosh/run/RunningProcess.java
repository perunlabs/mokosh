package com.perunlabs.mokosh.run;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.run.EntangledRunning.entangle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

public class RunningProcess implements Running<Void> {
  private final Process process;
  private final Running<Void> pumping;

  private RunningProcess(Process process, Running<Void> pumping) {
    this.process = process;
    this.pumping = pumping;
  }

  public static Running<Void> run(
      ProcessBuilder processBuilder,
      InputStream stdin,
      OutputStream stdout,
      OutputStream stderr) {

    check(processBuilder != null);
    check(stdin != null);
    check(stdout != null);
    check(stderr != null);

    AtomicBoolean broken = new AtomicBoolean(false);
    Process process;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw wrap(e);
    }

    Running<Void> pumpingStdin = run(() -> {
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
    });
    Running<Void> pumpingStdout = run(() -> {
      try (InputStream processStdout = process.getInputStream()) {
        pump(processStdout, stdout);
      } finally {
        if (!broken.get()) {
          stdout.close();
        }
      }
    });
    Running<Void> pumpingStderr = run(() -> {
      try (InputStream processStderr = process.getErrorStream()) {
        pump(processStderr, stderr);
      } finally {
        if (!broken.get()) {
          stderr.close();
        }
      }
    });
    Running<Void> pumping = entangle(pumpingStdin, pumpingStdout, pumpingStderr);
    return new RunningProcess(process, pumping);
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

  private static void pump(InputStream input, OutputStream output) {
    try {
      int count;
      byte[] buffer = new byte[8 * 1024];
      while ((count = input.read(buffer)) != -1) {
        if (Thread.interrupted()) {
          throw new AbortException();
        }
        output.write(buffer, 0, count);
      }
    } catch (IOException e) {
      throw wrap(e);
    }
  }

  private static Running<Void> run(IoOperation operation) {
    return Run.run(() -> {
      try {
        operation.invoke();
      } catch (IOException e) {
        throw wrap(e);
      }
    });
  }

  private static interface IoOperation {
    void invoke() throws IOException;
  }

  private static RuntimeException wrap(IOException exception) {
    if (exception instanceof ClosedByInterruptException) {
      return new AbortException(exception);
    } else if (exception instanceof InterruptedIOException) {
      return new AbortException(exception);
    } else {
      return new UncheckedIOException(exception);
    }
  }
}

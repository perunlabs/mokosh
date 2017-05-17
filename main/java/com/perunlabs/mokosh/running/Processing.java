package com.perunlabs.mokosh.running;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Streams.pump;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;
import static com.perunlabs.mokosh.running.Entangling.entangle;
import static com.perunlabs.mokosh.running.Supplying.supplying;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

public class Processing implements Running<Void> {
  private final Process process;
  private final Running<Void> pumping;

  private Processing(Process process, Running<Void> pumping) {
    this.process = process;
    this.pumping = pumping;
  }

  public static Running<Void> processing(
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
    Running<Void> pumpingStdout = supplying(unchecked(() -> {
      try (InputStream processStdout = process.getInputStream()) {
        pump(processStdout, stdout);
      } finally {
        if (!broken.get()) {
          stdout.close();
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
    Running<Void> pumping = entangle(pumpingStdin, pumpingStdout, pumpingStderr);
    return new Processing(process, pumping);
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
}

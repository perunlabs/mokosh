package com.perunlabs.mokosh.running;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Lambdas.asSupplier;
import static java.lang.String.format;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

public class Supplying {
  private static final AtomicInteger counter = new AtomicInteger(0);

  public static <T> Running<T> supplying(Supplier<T> code) {
    check(code != null);
    CountDownLatch executedLatch = new CountDownLatch(1);
    AtomicReference<Supplier<T>> executed = new AtomicReference<>();
    Thread thread = new Thread(() -> {
      try {
        T returned = code.get();
        executed.set(() -> returned);
      } catch (RuntimeException | Error e) {
        executed.set(() -> {
          throw e;
        });
      } finally {
        executedLatch.countDown();
      }
    });
    thread.setName(format("%s-%s",
        Thread.currentThread().getName(),
        counter.getAndIncrement()));
    thread.start();
    return new Running<T>() {
      public Supplier<T> await() {
        try {
          executedLatch.await();
          return executed.get();
        } catch (InterruptedException e) {
          throw new AbortException(e);
        }
      }

      public Running<T> abort() {
        thread.interrupt();
        return this;
      }

      public boolean isRunning() {
        return executedLatch.getCount() > 0;
      }
    };
  }

  public static Running<Void> supplying(Runnable code) {
    check(code != null);
    return supplying(asSupplier(code));
  }
}

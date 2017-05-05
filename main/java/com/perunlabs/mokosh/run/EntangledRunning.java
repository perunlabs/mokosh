package com.perunlabs.mokosh.run;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.run.Run.run;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

public class EntangledRunning<T> implements Running<T> {
  private final Running<T> resulting;
  private final List<Running<?>> allRunnings;

  private EntangledRunning(Running<T> resulting, List<Running<?>> allRunnings) {
    this.resulting = resulting;
    this.allRunnings = allRunnings;
  }

  public static <T> Running<T> entangle(Running<T> resulting, Running<?>... runnings) {
    check(resulting != null);
    check(runnings != null);
    List<Running<?>> allRunnings = new ArrayList<>();
    allRunnings.add(resulting);
    allRunnings.addAll(asList(runnings));
    check(!allRunnings.contains(null));
    return new EntangledRunning<>(resulting, allRunnings);
  }

  public static Running<Void> entangle(List<? extends Running<?>> runnings) {
    check(runnings != null);
    List<Running<?>> allRunnings = new ArrayList<>(runnings);
    check(!allRunnings.contains(null));
    Running<Void> resulting = run(() -> {});
    return new EntangledRunning<>(resulting, allRunnings);
  }

  public Supplier<T> await() {
    allRunnings.forEach(running -> {
      run(() -> {
        if (failed(running.await())) {
          allRunnings.forEach(Running::abort);
        }
      });
    });
    try {
      allRunnings.forEach(Running::await);
    } catch (AbortException e) {
      abort();
      throw e;
    }
    return resulting.await();
  }

  public Running<T> abort() {
    allRunnings.forEach(Running::abort);
    return this;
  }

  public boolean isRunning() {
    return allRunnings.stream()
        .anyMatch(Running::isRunning);
  }

  private static boolean failed(Supplier<?> result) {
    try {
      result.get();
    } catch (Throwable e) {
      return true;
    }
    return false;
  }
}

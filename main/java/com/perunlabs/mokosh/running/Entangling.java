package com.perunlabs.mokosh.running;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;

public class Entangling<T> implements Running<T> {
  private final Running<T> resulting;
  private final List<Running<?>> allRunnings;

  private Entangling(Running<T> resulting, List<Running<?>> allRunnings) {
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
    return new Entangling<>(resulting, allRunnings);
  }

  public static Running<Void> entangle(List<? extends Running<?>> runnings) {
    check(runnings != null);
    List<Running<?>> allRunnings = new ArrayList<>(runnings);
    check(!allRunnings.contains(null));
    Running<Void> resulting = supplying(() -> {});
    return new Entangling<>(resulting, allRunnings);
  }

  public Supplier<T> await() {
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

  public String toString() {
    return allRunnings.stream()
        .map(Object::toString)
        .collect(joining(", ", "entangle(", ")"));
  }
}

package com.perunlabs.mokosh.iterating;

import static com.perunlabs.mokosh.MokoshException.check;
import static java.lang.String.format;

import java.util.Iterator;
import java.util.function.Supplier;

import com.perunlabs.mokosh.running.Running;

public class Delegating<E> implements Iterating<E> {
  private final Running<?> running;
  private final Iterator<E> iterator;

  private Delegating(Running<?> running, Iterator<E> iterator) {
    this.running = running;
    this.iterator = iterator;
  }

  public static <E> Iterating<E> iterating(Running<?> running, Iterator<E> iterator) {
    check(running != null);
    check(iterator != null);
    return new Delegating<>(running, iterator);
  }

  public Supplier<Void> await() {
    return nullify(running.await());
  }

  public Running<Void> abort() {
    running.abort();
    return this;
  }

  public boolean isRunning() {
    return running.isRunning();
  }

  public boolean hasNext() {
    return iterator.hasNext();
  }

  public E next() {
    return iterator.next();
  }

  public String toString() {
    return format("iterating(%s, %s)", running, iterator);
  }

  private static Supplier<Void> nullify(Supplier<?> supplier) {
    return () -> {
      supplier.get();
      return null;
    };
  }
}

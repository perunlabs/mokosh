package com.perunlabs.mokosh.iterating;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static java.lang.String.format;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.running.Running;

public class Working<E> implements Iterating<E> {
  private final Lock lock = new ReentrantLock();
  private final Condition untilChange = lock.newCondition();

  private final Running<Void> supplying;
  private Optional<E> slot = Optional.empty();
  private boolean closed;

  private Working(Iterator<E> iterator) {
    supplying = supplying(() -> {
      lock.lock();
      try {
        while (true) {
          E next;
          lock.unlock();
          try {
            if (!iterator.hasNext()) {
              break;
            }
            next = iterator.next();
          } finally {
            lock.lock();
          }
          while (slot.isPresent()) {
            await(untilChange);
          }
          slot = Optional.of(next);
          untilChange.signal();
        }
        while (slot.isPresent()) {
          await(untilChange);
        }
        closed = true;
        untilChange.signal();
      } finally {
        lock.unlock();
      }
    });
  }

  public static <E> Iterating<E> working(Iterator<E> iterator) {
    check(iterator != null);
    return new Working<E>(iterator) {
      public String toString() {
        return format("working(%s)", iterator);
      }
    };
  }

  public Supplier<Void> await() {
    return supplying.await();
  }

  public Running<Void> abort() {
    return supplying.abort();
  }

  public boolean isRunning() {
    return supplying.isRunning();
  }

  public boolean hasNext() {
    lock.lock();
    try {
      while (true) {
        if (slot.isPresent()) {
          return true;
        }
        if (!slot.isPresent() && closed) {
          return false;
        }
        await(untilChange);
      }
    } finally {
      lock.unlock();
    }
  }

  public E next() {
    lock.lock();
    try {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      E next = slot.get();
      slot = Optional.empty();
      untilChange.signal();
      return next;
    } finally {
      lock.unlock();
    }
  }

  private static void await(Condition condition) {
    try {
      condition.await();
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
  }
}

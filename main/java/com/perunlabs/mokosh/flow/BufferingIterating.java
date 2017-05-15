package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.run.Run.run;
import static java.lang.String.format;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.run.Running;

public class BufferingIterating<E> implements Iterating<E> {
  private final Lock lock = new ReentrantLock();
  private final Condition untilChange = lock.newCondition();

  private final Running<Void> iterating;

  private final List<E> queue = new LinkedList<>();
  private boolean closed;

  private BufferingIterating(int limit, Iterator<E> iterator) {
    iterating = run(() -> {
      lock.lock();
      try {
        while (true) {
          lock.unlock();
          try {
            if (!iterator.hasNext()) {
              break;
            }
          } finally {
            lock.lock();
          }

          while (queue.size() == limit) {
            await(untilChange);
          }
          E next;
          lock.unlock();
          try {
            next = iterator.next();
          } finally {
            lock.lock();
          }
          queue.add(next);
          untilChange.signal();
        }
        closed = true;
        untilChange.signal();
      } finally {
        lock.unlock();
      }
    });
  }

  public static <E> Iterating<E> buffering(int limit, Iterator<E> iterator) {
    check(limit > 0);
    check(iterator != null);
    return new BufferingIterating(limit, iterator) {
      public String toString() {
        return format("buffering(%s, %s)", limit, iterator);
      }
    };
  }

  public Supplier<Void> await() {
    return iterating.await();
  }

  public Running<Void> abort() {
    return iterating.abort();
  }

  public boolean isRunning() {
    return iterating.isRunning();
  }

  public boolean hasNext() {
    lock.lock();
    try {
      while (true) {
        if (!queue.isEmpty()) {
          return true;
        }
        if (queue.isEmpty() && closed) {
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
      check(hasNext());
      while (queue.isEmpty()) {
        await(untilChange);
      }
      E next = queue.remove(0);
      untilChange.signal();
      return next;
    } finally {
      lock.unlock();
    }
  }

  private void await(Condition condition) {
    try {
      condition.await();
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
  }
}

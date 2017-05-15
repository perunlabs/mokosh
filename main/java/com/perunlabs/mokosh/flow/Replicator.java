package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.CachingIterator.caching;
import static java.lang.String.format;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.perunlabs.mokosh.AbortException;

public class Replicator<E> implements Iterable<E> {
  private final int count;
  private final Iterator<E> mainIterator;

  private final Lock lock = new ReentrantLock();
  private CountDownLatch latch;
  private final Set<Iterator<E>> iterators = new HashSet<>();
  private final Set<Iterator<E>> awaitingIterators = new HashSet<>();
  private E next;

  private Replicator(int count, Iterator<E> mainIterator) {
    this.count = count;
    this.mainIterator = mainIterator;
    latch = new CountDownLatch(count);
  }

  public static <E> Iterable<E> replicate(int count, Iterator<E> iterator) {
    check(count > 0);
    check(iterator != null);
    return new Replicator<E>(count, caching(iterator));
  }

  public Iterator<E> iterator() {
    lock.lock();
    try {
      check(iterators.size() < count);
      ReplicatorIterator iterator = new ReplicatorIterator();
      iterators.add(iterator);
      if (latch.getCount() == 1) {
        awaitingIterators.clear();
        latch.countDown();
        latch = new CountDownLatch(count);
      } else {
        latch.countDown();
        lock.unlock();
        try {
          await(latch);
        } finally {
          lock.lock();
        }
      }
      return iterator;
    } finally {
      lock.unlock();
    }
  }

  private class ReplicatorIterator implements Iterator<E> {
    public boolean hasNext() {
      return mainIterator.hasNext();
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      lock.lock();
      try {
        check(awaitingIterators.add(this));
        if (latch.getCount() == 1) {
          next = mainIterator.next();
          awaitingIterators.clear();
          latch.countDown();
          latch = new CountDownLatch(count);
        } else {
          latch.countDown();
          lock.unlock();
          try {
            await(latch);
          } finally {
            lock.lock();
          }
        }
        return next;
      } finally {
        lock.unlock();
      }
    }

    public String toString() {
      return format("%s.iterator()", Replicator.this);
    }
  }

  public String toString() {
    return format("replicate(%s, %s)", count, mainIterator);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
  }
}

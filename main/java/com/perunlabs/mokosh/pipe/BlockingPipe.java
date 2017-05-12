package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.MokoshException.check;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.perunlabs.mokosh.AbortException;

public class BlockingPipe<T> implements Pipe<T> {
  private final Lock lock = new ReentrantLock();
  private final Condition untilConnected = lock.newCondition();
  private final Condition untilQueueHasElements = lock.newCondition();
  private final Condition untilQueueHasSpace = lock.newCondition();

  private final List<T> queue = new LinkedList<>();
  private final int queueLimit;
  private boolean connected;
  private boolean closed;

  private BlockingPipe(int queueLimit) {
    this.queueLimit = queueLimit;
  }

  public static <T> Pipe<T> blockingPipe(int queueLimit) {
    check(queueLimit >= 1);
    return new BlockingPipe<T>(queueLimit);
  }

  public Iterator<T> input() {
    return new Iterator<T>() {
      public boolean hasNext() {
        lock.lock();
        try {
          while (!connected) {
            await(untilConnected);
          }
          return !(queue.isEmpty() && closed);
        } finally {
          lock.unlock();
        }
      }

      public T next() {
        lock.lock();
        try {
          check(hasNext());
          while (queue.isEmpty()) {
            await(untilQueueHasElements);
          }
          untilQueueHasSpace.signal();
          return queue.remove(0);
        } finally {
          lock.unlock();
        }
      }
    };
  }

  public Output<T> output() {
    return new Output<T>() {
      public void connect(Iterator<? extends T> iterator) {
        check(iterator != null);
        lock.lock();
        try {
          check(!connected);
          connected = true;
          untilConnected.signal();
          while (iterator.hasNext()) {
            while (queue.size() >= queueLimit) {
              await(untilQueueHasSpace);
            }
            T next;
            lock.unlock();
            try {
              next = iterator.next();
            } finally {
              lock.lock();
            }
            queue.add(next);
            untilQueueHasElements.signal();
          }
          closed = true;
        } finally {
          lock.unlock();
        }
      }
    };
  }

  private void await(Condition condition) {
    try {
      condition.await();
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
  }
}

package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.MokoshException.check;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.perunlabs.mokosh.AbortException;

public class MultiPipe<T> implements Pipe<T> {
  private final Lock lock = new ReentrantLock();
  private final Condition untilConnected = lock.newCondition();
  private final Condition untilQueueHasElements = lock.newCondition();
  private final Condition untilQueueHasSpace = lock.newCondition();

  private final List<List<T>> queues = new LinkedList<>();
  private boolean connected;
  private boolean closed;

  private MultiPipe() {}

  public static <T> Pipe<T> multiPipe() {
    return new MultiPipe<T>();
  }

  public Iterator<T> input() {
    List<T> queue;
    lock.lock();
    try {
      check(!connected);
      queue = new ArrayList<>();
      queues.add(queue);
    } finally {
      lock.unlock();
    }
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
          check(queues.size() > 0);
          connected = true;
          untilConnected.signalAll();
          while (iterator.hasNext()) {
            while (!queues.stream().allMatch(List<T>::isEmpty)) {
              await(untilQueueHasSpace);
            }
            T next;
            lock.unlock();
            try {
              next = iterator.next();
            } finally {
              lock.lock();
            }
            queues.forEach(queue -> queue.add(next));
            untilQueueHasElements.signalAll();
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

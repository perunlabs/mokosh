package com.perunlabs.mokosh.run.testing;

import com.perunlabs.mokosh.AbortException;

public class Testing {
  public static Void interruptMeAfterSeconds(double seconds) {
    Thread caller = Thread.currentThread();
    Thread interrupter = new Thread(() -> {
      sleepSeconds(seconds);
      caller.interrupt();
    });
    interrupter.start();
    return null;
  }

  public static void sleepSeconds(double seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
  }
}

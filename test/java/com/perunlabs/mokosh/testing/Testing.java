package com.perunlabs.mokosh.testing;

import static java.lang.String.format;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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

  public static Matcher<Throwable> withMessage(Matcher<String> messageMatcher) {
    return new TypeSafeMatcher<Throwable>() {
      protected boolean matchesSafely(Throwable item) {
        return messageMatcher.matches(item.getMessage());
      }

      public void describeTo(Description description) {
        description.appendText(format("withMessage(%s)", messageMatcher));
      }
    };
  }
}

package com.perunlabs.mokosh.testing;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  public static Void sleepSeconds(double seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (InterruptedException e) {
      throw new AbortException(e);
    }
    return null;
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

  public static <E> List<E> collectToList(Iterator<E> iterator) {
    List<E> list = new ArrayList<>();
    iterator.forEachRemaining(list::add);
    return list;
  }
}

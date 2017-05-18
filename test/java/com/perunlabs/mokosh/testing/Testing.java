package com.perunlabs.mokosh.testing;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.testory.proxy.Handler;
import org.testory.proxy.Invocation;

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

  public static Matcher<Throwable> causedBy(Matcher<Throwable> causeMatcher) {
    return new TypeSafeMatcher<Throwable>() {
      protected boolean matchesSafely(Throwable throwable) {
        return causeMatcher.matches(throwable.getCause());
      }

      public void describeTo(Description description) {
        description.appendText(format("causedBy(%s)", causeMatcher));
      }
    };
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

  public static byte[] bytes(int count) {
    byte[] bytes = new byte[count];
    for (int index = 0; index < bytes.length; index++) {
      bytes[index] = (byte) index;
    }
    return bytes;
  }

  public static Handler willSleepSeconds(double seconds) {
    return new Handler() {
      public Object handle(Invocation invocation) throws Throwable {
        sleepSeconds(seconds);
        return null;
      }
    };
  }

  public static byte[] readAllBytes(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int oneByte;
    while ((oneByte = input.read()) != -1) {
      output.write(oneByte);
    }
    return output.toByteArray();
  }
}

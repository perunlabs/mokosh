package com.perunlabs.mokosh.run;

import static com.perunlabs.mokosh.run.Run.run;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestRun {
  @Rule
  public final Timeout timeout = seconds(1);

  private Object object;
  private RuntimeException runtimeException;
  private Running<?> running, otherRunning;
  private Supplier<?> result;
  private CountDownLatch latch;
  private Thread thread;

  @Before
  public void before() {
    givenTest(this);
  }

  @After
  public void after() {
    running.abort();
    otherRunning.abort();
  }

  @Test
  public void supplies_returned_object() {
    given(running = run(() -> object));
    when(running.await().get());
    thenReturned(object);
  }

  @Test
  public void supplies_thrown_exception() {
    given(running = run(() -> {
      throw runtimeException;
    }));
    given(result = running.await());
    when(() -> result.get());
    thenThrown(runtimeException);
  }

  @Test
  public void awaits_completion_returning_object() {
    given(running = run(() -> object));
    when(running.await());
    thenReturned();
  }

  @Test
  public void awaits_completion_throwing_throwable() {
    given(running = run(() -> {
      throw new RuntimeException();
    }));
    when(running.await());
    thenReturned();
  }

  @Test
  public void awaiting_is_abortable() {
    given(latch = new CountDownLatch(1));
    given(running = run(() -> {
      await(latch);
    }));
    given(thread = start(new Thread(() -> running.await())));
    when(() -> {
      thread.interrupt();
      thread.join();
    });
    thenReturned();
  }

  @Test
  public void runs_in_parallel() {
    given(latch = new CountDownLatch(2));
    given(running = run(() -> {
      latch.countDown();
      await(latch);
    }));
    given(otherRunning = run(() -> {
      latch.countDown();
      await(latch);
    }));
    when(() -> {
      running.await().get();
      otherRunning.await().get();
    });
    thenReturned();
  }

  @Test
  public void is_running() {
    given(latch = new CountDownLatch(1));
    given(running = run(() -> await(latch)));
    when(running.isRunning());
    thenReturned(true);
  }

  @Test
  public void is_not_running_if_completed() {
    given(running = run(() -> {}));
    given(running.await());
    when(running.isRunning());
    thenReturned(false);
  }

  @Test
  public void aborts_if_running() {
    given(latch = new CountDownLatch(1));
    given(running = run(() -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new AbortException(e);
      }
    }));
    given(result = running.abort().await());
    when(() -> result.get());
    thenThrown(AbortException.class);
  }

  @Test
  public void aborting_has_no_effect_if_not_running() {
    given(running = run(() -> object));
    given(running.await());
    when(running.abort().await().get());
    thenReturned(object);
  }

  @Test
  public void aborted_is_not_running() {
    given(latch = new CountDownLatch(1));
    given(running = run(() -> await(latch)));
    given(running.abort().await());
    when(running.isRunning());
    thenReturned(false);
  }

  @Test
  public void abort_returns_this() {
    given(running = run(() -> object));
    when(running.abort());
    thenReturned(running);
  }

  @Test
  public void uses_parent_thread_name() {
    when(run(() -> Thread.currentThread().getName()).await().get());
    thenReturned(startsWith(Thread.currentThread().getName() + "-"));
  }

  @Test
  public void checks_null_supplier() {
    when(() -> run((Supplier<Object>) null));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_null_runnable() {
    when(() -> run((Runnable) null));
    thenThrown(MokoshException.class);
  }

  private static Thread start(Thread thread) {
    thread.setUncaughtExceptionHandler((t, e) -> {});
    thread.start();
    return thread;
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException();
    }
  }
}

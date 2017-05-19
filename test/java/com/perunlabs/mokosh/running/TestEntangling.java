package com.perunlabs.mokosh.running;

import static com.perunlabs.mokosh.running.Entangling.entangle;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.sleepSeconds;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.givenTry;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.List;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestEntangling {
  @Rule
  public final Timeout timeout = seconds(1);

  private Running<?> running, runningA, runningB, runningC;
  private Supplier<?> result;
  private List<Object> log;

  @Before
  public void before() {
    givenTest(this);
  }

  @After
  public void after() {
    running.abort();
    runningA.abort();
    runningB.abort();
    runningC.abort();
  }

  @Test
  public void awaits_for_all() {
    given(runningA = supplying(() -> {
      sleepSeconds(0.1);
      log.add(1);
    }));
    given(runningB = supplying(() -> {
      sleepSeconds(0.1);
      log.add(2);
    }));
    given(running = entangle(runningA, runningB));
    when(running.await());
    thenCalled(log).add(1);
    thenCalled(log).add(2);
  }

  @Test
  public void awaits_for_all_in_list() {
    given(runningA = supplying(() -> {
      sleepSeconds(0.1);
      log.add(1);
    }));
    given(runningB = supplying(() -> {
      sleepSeconds(0.1);
      log.add(2);
    }));
    given(running = entangle(asList(runningA, runningB)));
    when(running.await());
    thenCalled(log).add(1);
    thenCalled(log).add(2);
  }

  @Test
  public void awaiting_is_abortable() {
    given(running = entangle(supplying(() -> {
      sleepSeconds(100);
    })));
    given(interruptMeAfterSeconds(0.1));
    when(() -> running.await());
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_all() {
    given(running = entangle(runningA, runningB));
    when(running.abort());
    thenCalled(runningA).abort();
    thenCalled(runningB).abort();
    thenReturned();
  }

  @Test
  public void aborts_if_waiter_is_aborter() {
    given(running = entangle(supplying(() -> {
      sleepSeconds(100);
    })));
    given(interruptMeAfterSeconds(0.1));
    givenTry(running).await();
    given(result = running.await());
    when(() -> result.get());
    thenThrown(AbortException.class);
  }

  @Test
  public void abort_returns_this() {
    given(running = entangle(runningA, runningB));
    when(running.abort());
    thenReturned(running);
  }

  @Test
  public void aborts_all_in_list() {
    given(running = entangle(asList(runningA, runningB)));
    when(running.abort());
    thenCalled(runningA).abort();
    thenCalled(runningB).abort();
    thenReturned();
  }

  @Test
  public void abort_returns_this_for_list() {
    given(running = entangle(asList(runningA, runningB)));
    when(running.abort());
    thenReturned(running);
  }

  @Test
  public void result_is_from_first() {
    given(running = entangle(runningA, runningB));
    given(willReturn(result), runningA).await();
    when(running.await());
    thenReturned(result);
  }

  @Test
  public void result_is_null_for_list() {
    given(running = entangle(asList(runningA, runningB)));
    given(willReturn(result), runningA).await();
    given(willReturn(result), runningB).await();
    when(running.await().get());
    thenReturned(null);
  }

  @Test
  public void is_running_if_first_is_running() {
    given(willReturn(true), runningA).isRunning();
    given(willReturn(false), runningB).isRunning();
    given(running = entangle(runningA, runningB));
    when(running.isRunning());
    thenReturned(true);
  }

  @Test
  public void is_running_if_second_is_running() {
    given(willReturn(false), runningA).isRunning();
    given(willReturn(true), runningB).isRunning();
    given(running = entangle(runningA, runningB));
    when(running.isRunning());
    thenReturned(true);
  }

  @Test
  public void is_not_running_if_none_is_running() {
    given(willReturn(false), runningA).isRunning();
    given(willReturn(false), runningB).isRunning();
    given(running = entangle(runningA, runningB));
    when(running.isRunning());
    thenReturned(false);
  }

  @Test
  public void implements_to_string() {
    given(running = entangle(runningA, runningB, runningC));
    when(running.toString());
    thenReturned(format("entangle(%s, %s, %s)", runningA, runningB, runningC));
  }

  /** should compile */
  public void type_is_from_first() {
    Running<String> stringExecuting = supplying(() -> "");
    Running<String> joined = entangle(stringExecuting, runningA);
    joined.toString();
  }

  /** should compile */
  public void type_of_list_may_be_specific() {
    Running<String> stringExecuting = supplying(() -> "");
    List<Running<String>> executings = asList(stringExecuting);
    Running<Void> joined = entangle(executings);
    joined.toString();
  }

  @Test
  public void checks_for_null_main() {
    when(() -> entangle(null, new Running[0]));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_for_null_in_array() {
    when(() -> entangle(runningA, null, runningB));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_for_null_in_list() {
    when(() -> entangle(asList(runningA, null, runningB)));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_for_null_list() {
    when(() -> entangle((List<Running<?>>) null));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_for_null_varargs() {
    when(() -> entangle(runningA, (Running<?>[]) null));
    thenThrown(MokoshException.class);
  }
}

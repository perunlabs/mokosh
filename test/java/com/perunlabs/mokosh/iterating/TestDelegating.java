package com.perunlabs.mokosh.iterating;

import static com.perunlabs.mokosh.iterating.Delegating.iterating;
import static java.lang.String.format;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.Iterator;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.perunlabs.mokosh.MokoshException;
import com.perunlabs.mokosh.running.Running;

public class TestDelegating {
  private Iterator<Foo> iterator;
  private Running<Void> running;
  private Iterating<Foo> iterating;
  private Foo foo;

  @Before
  public void before() {
    givenTest(this);
  }

  @Test
  public void delegates_await() {
    given(iterating = iterating(running, iterator));
    when(iterating.await());
    thenCalled(running).await();
  }

  @Test
  public void delegates_abort() {
    given(iterating = iterating(running, iterator));
    when(iterating.abort());
    thenCalled(running).abort();
  }

  @Test
  public void delegates_is_running() {
    given(iterating = iterating(running, iterator));
    given(willReturn(true), running).isRunning();
    when(iterating.isRunning());
    thenReturned(true);
    thenCalled(running).isRunning();
  }

  @Test
  public void delegates_has_next() {
    given(iterating = iterating(running, iterator));
    given(willReturn(true), iterator).hasNext();
    when(iterating.hasNext());
    thenReturned(true);
    thenCalled(iterator).hasNext();
  }

  @Test
  public void delegates_next() {
    given(iterating = iterating(running, iterator));
    given(willReturn(foo), iterator).next();
    when(iterating.next());
    thenCalled(iterator).next();
  }

  @Test
  public void implements_to_string() {
    given(iterating = iterating(running, iterator));
    when(iterating.toString());
    thenReturned(format("iterating(%s, %s)", running, iterator));
  }

  @Test
  public void nullifies_result() {
    given(willReturn((Supplier<Foo>) () -> foo), running).await();
    given(iterating = iterating(running, iterator));
    when(iterating.await().get());
    thenReturned(null);
  }

  @Test
  public void checks_that_running_is_not_null() {
    given(running = null);
    when(() -> iterating(running, iterator));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_that_iterator_is_not_null() {
    given(iterator = null);
    when(() -> iterating(running, iterator));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

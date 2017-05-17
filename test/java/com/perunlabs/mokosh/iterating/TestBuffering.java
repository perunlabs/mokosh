package com.perunlabs.mokosh.iterating;

import static com.perunlabs.mokosh.iterating.Buffering.buffering;
import static com.perunlabs.mokosh.testing.Testing.collectToList;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.willSleepSeconds;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Stream.iterate;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.onInstance;
import static org.testory.Testory.then;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestBuffering {
  @Rule
  public final Timeout timeout = seconds(1);

  private Iterating<Foo> iterating;
  private Foo a, b, c;
  private Iterator<Foo> iterator;

  @Before
  public void before() {
    givenTest(this);
  }

  @Test
  public void pipes_one_element() {
    given(iterating = buffering(1, asList(a).iterator()));
    when(collectToList(iterating));
    thenReturned(asList(a));
  }

  @Test
  public void pipes_many_elements() {
    given(iterating = buffering(1, asList(a, b, c).iterator()));
    when(collectToList(iterating));
    thenReturned(asList(a, b, c));
  }

  @Test
  public void reads_after_iterating_completed() {
    given(iterating = buffering(3, asList(a).iterator()));
    given(iterating.await());
    when(collectToList(iterating));
    thenReturned(asList(a));
  }

  @Test
  public void aborts_has_next() {
    given(willSleepSeconds(1), iterator).hasNext();
    given(iterating = buffering(1, iterator));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.hasNext());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_next() {
    given(willReturn(true), iterator).hasNext();
    given(willSleepSeconds(1), iterator).next();
    given(iterating = buffering(1, iterator));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.next());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_awaiting() {
    given(iterating = buffering(1, asList(a, b, c).iterator()));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.await());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_running_while_iterating() {
    given(iterating = buffering(1_000_000, iterate(a, i -> i).iterator()));
    when(iterating.abort().await());
    thenReturned();
    then(!iterating.isRunning());
  }

  @Test
  public void aborts_running_while_blocked() {
    given(willSleepSeconds(0.2), onInstance(iterator));
    given(iterating = buffering(1, iterator));
    when(iterating.abort().await());
    thenReturned();
    then(!iterating.isRunning());
  }

  @Test
  public void implements_to_string() {
    given(iterating = buffering(2, iterator));
    when(iterating.toString());
    thenReturned(format("buffering(%s, %s)", 2, iterator));
  }

  @Test
  public void checks_that_size_is_positive() {
    when(() -> buffering(0, iterator));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_that_iterator_is_not_null() {
    when(() -> buffering(1, null));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

package com.perunlabs.mokosh.iterating;

import static com.perunlabs.mokosh.iterating.Working.working;
import static com.perunlabs.mokosh.testing.Testing.collectToList;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.sleepSeconds;
import static com.perunlabs.mokosh.testing.Testing.willSleepSeconds;
import static java.lang.String.format;
import static java.util.Arrays.asList;
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

public class TestWorking {
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
    given(iterating = working(asList(a).iterator()));
    when(collectToList(iterating));
    thenReturned(asList(a));
  }

  @Test
  public void pipes_many_elements() {
    given(iterating = working(asList(a, b, c).iterator()));
    when(collectToList(iterating));
    thenReturned(asList(a, b, c));
  }

  @Test
  public void awaits_until_last_element_is_read() {
    given(iterating = working(asList(a).iterator()));
    given(sleepSeconds(0.1));
    when(iterating.isRunning());
    thenReturned(true);
  }

  @Test
  public void aborts_has_next() {
    given(willSleepSeconds(1), iterator).hasNext();
    given(iterating = working(iterator));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.hasNext());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_next() {
    given(willReturn(true), iterator).hasNext();
    given(willSleepSeconds(1), iterator).next();
    given(iterating = working(iterator));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.next());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_awaiting() {
    given(iterating = working(asList(a, b).iterator()));
    given(interruptMeAfterSeconds(0.1));
    when(() -> iterating.await());
    thenThrown(AbortException.class);
    then(iterating.isRunning());
  }

  @Test
  public void aborts_running() {
    given(willSleepSeconds(0.2), onInstance(iterator));
    given(iterating = working(iterator));
    when(iterating.abort().await());
    thenReturned();
    then(!iterating.isRunning());
  }

  @Test
  public void implements_to_string() {
    given(iterating = working(iterator));
    when(iterating.toString());
    thenReturned(format("working(%s)", iterator));
  }

  @Test
  public void checks_that_iterator_is_not_null() {
    when(() -> working(null));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

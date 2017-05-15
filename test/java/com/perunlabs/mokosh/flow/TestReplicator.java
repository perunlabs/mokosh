package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.flow.Replicator.replicate;
import static com.perunlabs.mokosh.run.Run.run;
import static com.perunlabs.mokosh.testing.Testing.collectToList;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.sleepSeconds;
import static com.perunlabs.mokosh.testing.Testing.willSleepSeconds;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.spy;
import static org.testory.Testory.thenCalledTimes;
import static org.testory.Testory.thenEqual;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;
import com.perunlabs.mokosh.run.Running;

public class TestReplicator {
  @Rule
  public final Timeout timeout = seconds(1);

  private Foo a, b, c;
  private Iterator<Foo> iterator;
  private Iterable<Foo> replicator;
  private Running<?> runningA, runningB;

  @Before
  public void before() {
    givenTest(this);
  }

  @After
  public void after() {
    runningA.abort();
    runningB.abort();
  }

  @Test
  public void replicates_once() {
    given(iterator = asList(a, b, c).iterator());
    given(replicator = replicate(1, iterator));
    when(collectToList(replicator.iterator()));
    thenReturned(asList(a, b, c));
  }

  @Test
  public void replicates_twice() {
    given(iterator = asList(a, b, c).iterator());
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(runningB = run(() -> collectToList(replicator.iterator())));
    when(() -> {
      runningA.await();
      runningB.await();
    });
    thenEqual(runningA.await().get(), asList(a, b, c));
    thenEqual(runningB.await().get(), asList(a, b, c));
  }

  @Test
  public void iterator_blocks_until_all_iterators_are_requested() {
    given(iterator = asList(a, b, c).iterator());
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> replicator.iterator()));
    given(sleepSeconds(0.1));
    when(runningA.isRunning());
    thenReturned(true);
  }

  @Test
  public void has_next_is_called_minimum_number_of_times() {
    given(iterator = spy(asList(a, b, c).iterator()));
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(runningB = run(() -> collectToList(replicator.iterator())));
    when(() -> {
      runningA.await();
      runningB.await();
    });
    thenCalledTimes(4, iterator).hasNext();
  }

  @Test
  public void next_blocks_until_all_iterators_call_next() {
    given(iterator = asList(a, b, c).iterator());
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(runningB = run(() -> replicator.iterator()));
    given(sleepSeconds(0.1));
    when(runningA.isRunning());
    thenReturned(true);
  }

  @Test
  public void cannot_get_more_iterators_than_count() {
    given(iterator = asList(a, b, c).iterator());
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(runningB = run(() -> collectToList(replicator.iterator())));
    when(() -> replicator.iterator());
    thenThrown(MokoshException.class);
  }

  @Test
  public void abort_when_blocked_on_has_next() {
    given(willSleepSeconds(1), iterator).hasNext();
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(interruptMeAfterSeconds(0.1));
    when(() -> runningA.await());
    thenThrown(AbortException.class);
  }

  @Test
  public void abort_when_blocked_on_next() {
    given(willReturn(true), iterator).hasNext();
    given(willSleepSeconds(1), iterator).next();
    given(replicator = replicate(2, iterator));
    given(runningA = run(() -> collectToList(replicator.iterator())));
    given(interruptMeAfterSeconds(0.1));
    when(() -> runningA.await());
    thenThrown(AbortException.class);
  }

  @Test
  public void implements_to_string() {
    given(replicator = replicate(2, iterator));
    when(replicator.toString());
    thenReturned(format("replicate(%s, caching(%s))", 2, iterator));
  }

  @Test
  public void iterator_implements_to_string() {
    given(replicator = replicate(1, iterator));
    when(replicator.iterator().toString());
    thenReturned(format("replicate(%s, caching(%s)).iterator()", 1, iterator));
  }

  @Test
  public void checks_if_count_is_positive() {
    when(() -> replicate(0, asList(a, b, c).iterator()));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_if_iterator_is_not_null() {
    when(() -> replicate(2, null));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

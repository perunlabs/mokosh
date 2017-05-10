package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.pipe.BlockingPipe.blockingPipe;
import static com.perunlabs.mokosh.run.Run.run;
import static com.perunlabs.mokosh.testing.Testing.collectToList;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.sleepSeconds;
import static java.util.Arrays.asList;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.spy;
import static org.testory.Testory.then;
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

public class TestBlockingPipe {
  @Rule
  public final Timeout timeout = seconds(1);

  private Foo a, b, c;
  private Iterator<Foo> iterator;
  private Pipe<Foo> pipe;
  private Running<?> inputting, outputting;

  @Before
  public void before() {
    givenTest(this);
  }

  @After
  public void after() {
    inputting.abort();
    outputting.abort();
  }

  @Test
  public void pipes_fewer_elements_than_buffer_size() {
    given(iterator = asList(a, b, c).iterator());
    given(pipe = blockingPipe(5));
    given(outputting = run(() -> pipe.output().connect(iterator)));
    when(collectToList(pipe.input()));
    thenReturned(asList(a, b, c));
  }

  @Test
  public void pipes_more_elements_than_buffer_size() {
    given(iterator = asList(a, b, c).iterator());
    given(pipe = blockingPipe(1));
    given(outputting = run(() -> pipe.output().connect(iterator)));
    when(collectToList(pipe.input()));
    thenReturned(asList(a, b, c));
  }

  @Test
  public void connects_input_before_output() {
    given(iterator = asList(a, b, c).iterator());
    given(pipe = blockingPipe(3));
    given(inputting = run(() -> collectToList(pipe.input())));
    given(sleepSeconds(0.1));
    given(pipe.output()).connect(iterator);
    when(inputting.await().get());
    thenReturned(asList(a, b, c));
  }

  @Test
  public void blocks_input_if_no_elements() {
    given(pipe = blockingPipe(1));
    given(inputting = run(() -> collectToList(pipe.input())));
    when(sleepSeconds(0.1));
    then(inputting.isRunning());
  }

  @Test
  public void blocks_output_if_no_space() {
    given(iterator = spy(asList(a, b, c).iterator()));
    given(pipe = blockingPipe(1));
    given(outputting = run(() -> pipe.output().connect(iterator)));
    when(sleepSeconds(0.1));
    then(outputting.isRunning());
  }

  @Test
  public void aborts_inputting() {
    given(willReturn(true), iterator).hasNext();
    given(willReturn(a), iterator).next();
    given(pipe = blockingPipe(1));
    given(outputting = run(() -> pipe.output().connect(iterator)));
    given(interruptMeAfterSeconds(0.1));
    when(() -> collectToList(pipe.input()));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_inputting_when_blocked() {
    given(pipe = blockingPipe(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> collectToList(pipe.input()));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_outputting() {
    given(willReturn(true), iterator).hasNext();
    given(willReturn(a), iterator).next();
    given(pipe = blockingPipe(1));
    given(inputting = run(() -> collectToList(pipe.input())));
    given(interruptMeAfterSeconds(0.1));
    when(() -> pipe.output().connect(iterator));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_outputting_when_blocked() {
    given(iterator = asList(a, b, c).iterator());
    given(pipe = blockingPipe(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> pipe.output().connect(iterator));
    thenThrown(AbortException.class);
  }

  @Test
  public void cannot_reconnect() {
    given(pipe = blockingPipe(3));
    given(() -> pipe.output().connect(iterator));
    when(() -> pipe.output().connect(iterator));
    thenThrown(MokoshException.class);
  }

  @Test
  public void cannot_use_zero_length_buffer() {
    when(() -> blockingPipe(0));
    thenThrown(MokoshException.class);
  }

  @Test
  public void cannot_connect_null() {
    given(pipe = blockingPipe(3));
    given(iterator = null);
    when(() -> pipe.output().connect(iterator));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

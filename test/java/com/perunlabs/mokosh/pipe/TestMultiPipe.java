package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.pipe.MultiPipe.multiPipe;
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
import static org.testory.Testory.thenEqual;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;
import com.perunlabs.mokosh.run.Running;

public class TestMultiPipe {
  @Rule
  public final Timeout timeout = seconds(1);

  private Pipe<Foo> pipe;
  private Iterator<Foo> input, otherInput;
  private Output<Foo> output;
  private Iterator<? extends Foo> iterator;
  private Foo a, b, c;
  private Running<List<Foo>> inputtingA, inputtingB;
  private Running<Void> outputting;

  @Before
  public void before() {
    givenTest(this);
    given(pipe = multiPipe());
  }

  @After
  public void after() {
    inputtingA.abort();
    inputtingB.abort();
    outputting.abort();
  }

  @Test
  public void pipes_elements_to_all_inputs() {
    given(input = pipe.input());
    given(otherInput = pipe.input());
    given(output = pipe.output());
    given(inputtingA = run(() -> collectToList(input)));
    given(inputtingB = run(() -> collectToList(otherInput)));
    when(() -> output.connect(asList(a, b, c).iterator()));
    thenEqual(inputtingA.await().get(), asList(a, b, c));
    thenEqual(inputtingB.await().get(), asList(a, b, c));
  }

  @Test
  public void inputs_cannot_be_connected_after_output() {
    given(pipe.input());
    given(output = pipe.output());
    given(pipe.input());
    given(run(() -> output.connect(asList(a, b, c).iterator())));
    given(sleepSeconds(0.1));
    when(() -> pipe.input());
    thenThrown(MokoshException.class);
  }

  @Test
  public void input_must_be_at_least_one() {
    given(output = pipe.output());
    when(() -> output.connect(asList(a, b, c).iterator()));
    thenThrown(MokoshException.class);
  }

  @Test
  public void blocks_input_if_no_elements() {
    given(inputtingA = run(() -> collectToList(pipe.input())));
    when(sleepSeconds(0.1));
    then(inputtingA.isRunning());
  }

  @Test
  public void blocks_output_if_no_space() {
    given(iterator = spy(asList(a, b, c).iterator()));
    given(pipe.input());
    given(outputting = run(() -> pipe.output().connect(iterator)));
    when(sleepSeconds(0.1));
    then(outputting.isRunning());
  }

  @Test
  public void blocks_output_if_no_space_in_one_of_queues() {
    given(input = pipe.input());
    given(otherInput = pipe.input());
    given(output = pipe.output());
    given(inputtingA = run(() -> collectToList(input)));
    given(outputting = run(() -> output.connect(asList(a, b, c).iterator())));
    when(sleepSeconds(0.1));
    then(outputting.isRunning());
  }

  @Test
  public void aborts_inputting() {
    given(willReturn(true), iterator).hasNext();
    given(willReturn(a), iterator).next();
    given(input = pipe.input());
    given(outputting = run(() -> pipe.output().connect(iterator)));
    given(interruptMeAfterSeconds(0.1));
    when(() -> collectToList(input));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_inputting_when_blocked() {
    given(interruptMeAfterSeconds(0.1));
    when(() -> collectToList(pipe.input()));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_outputting() {
    given(willReturn(true), iterator).hasNext();
    given(willReturn(a), iterator).next();
    given(inputtingA = run(() -> collectToList(pipe.input())));
    given(interruptMeAfterSeconds(0.1));
    when(() -> pipe.output().connect(iterator));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_outputting_when_blocked() {
    given(iterator = asList(a, b, c).iterator());
    given(interruptMeAfterSeconds(0.1));
    given(pipe.input());
    when(() -> pipe.output().connect(iterator));
    thenThrown(AbortException.class);
  }

  @Test
  public void cannot_reconnect() {
    given(pipe.input());
    given(() -> pipe.output().connect(iterator));
    when(() -> pipe.output().connect(iterator));
    thenThrown(MokoshException.class);
  }

  @Test
  public void cannot_connect_null() {
    given(iterator = null);
    when(() -> pipe.output().connect(iterator));
    thenThrown(MokoshException.class);
  }

  private static class Foo {}
}

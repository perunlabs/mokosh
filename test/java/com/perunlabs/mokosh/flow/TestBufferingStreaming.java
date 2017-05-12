package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.common.Unchecked.unchecked;
import static com.perunlabs.mokosh.flow.BufferingStreaming.buffering;
import static com.perunlabs.mokosh.testing.Testing.bytes;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.willSleepSeconds;
import static java.lang.String.format;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.onInstance;
import static org.testory.Testory.spy;
import static org.testory.Testory.then;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.thenCalledNever;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestBufferingStreaming {
  @Rule
  public final Timeout timeout = seconds(1);

  private InputStream input;
  private Streaming streaming;
  private byte[] bytes;

  @Before
  public void before() {
    givenTest(this);
    given(bytes = new byte[1]);
  }

  @After
  public void after() {
    streaming.abort();
  }

  @Test
  public void streams_single_byte() {
    given(streaming = buffering(100, new ByteArrayInputStream(bytes(1))));
    when(read(streaming));
    thenReturned(bytes(1));
  }

  @Test
  public void streams_many_bytes() {
    given(streaming = buffering(1000, new ByteArrayInputStream(bytes(1_000_000))));
    when(read(streaming));
    thenReturned(bytes(1_000_000));
  }

  @Test
  public void streams_many_bytes_buffered() {
    given(streaming = buffering(1_000_000, new ByteArrayInputStream(bytes(10_000_000))));
    when(readBuffered(streaming, 10_000));
    thenReturned(bytes(10_000_000));
  }

  @Test
  public void reads_after_buffering_completed() {
    given(streaming = buffering(1_000, new ByteArrayInputStream(bytes(500))));
    given(streaming.await());
    when(read(streaming));
    thenReturned(bytes(500));
  }

  @Test
  public void closes_input() throws IOException {
    given(input = spy(new ByteArrayInputStream(bytes(1))));
    given(streaming = buffering(1, input));
    when(read(streaming));
    thenCalled(input).close();
  }

  @Test
  public void aborts_reading_byte() {
    given(willSleepSeconds(0.2), onInstance(input));
    given(streaming = buffering(1, input));
    given(interruptMeAfterSeconds(0.1));
    when(() -> streaming.read());
    thenThrown(AbortException.class);
    then(streaming.isRunning());
  }

  @Test
  public void aborts_reading_array() {
    given(willSleepSeconds(0.2), onInstance(input));
    given(streaming = buffering(1, input));
    given(interruptMeAfterSeconds(0.1));
    when(() -> streaming.read(bytes));
    thenThrown(AbortException.class);
    then(streaming.isRunning());
  }

  @Test
  public void aborts_reading_subarray() {
    given(willSleepSeconds(0.2), onInstance(input));
    given(streaming = buffering(1, input));
    given(interruptMeAfterSeconds(0.1));
    when(() -> streaming.read(bytes, 0, 1));
    thenThrown(AbortException.class);
    then(streaming.isRunning());
  }

  @Test
  public void aborts_awaiting() {
    given(willSleepSeconds(0.2), onInstance(input));
    given(streaming = buffering(1, input));
    given(interruptMeAfterSeconds(0.1));
    when(() -> streaming.await());
    thenThrown(AbortException.class);
    then(streaming.isRunning());
  }

  @Test
  public void aborts_running_while_buffering() throws IOException {
    given(input = spy(new InputStream() {
      public int read() {
        return 0;
      }
    }));
    given(streaming = buffering(1_000_000, input));
    when(streaming.abort().await());
    then(!streaming.isRunning());
    thenCalledNever(input).close();
  }

  @Test
  public void aborts_running_while_blocked() throws IOException {
    given(willSleepSeconds(0.2), onInstance(input));
    given(streaming = buffering(1, input));
    when(streaming.abort().await());
    thenReturned();
    then(!streaming.isRunning());
  }

  @Test
  public void implements_to_string() {
    given(streaming = buffering(100, input));
    when(streaming.toString());
    thenReturned(format("buffering(%s, %s)", 100, input));
  }

  @Test
  public void checks_that_size_is_positive() {
    when(() -> buffering(0, input));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_that_input_is_not_null() {
    when(() -> buffering(1, null));
    thenThrown(MokoshException.class);
  }

  private static byte[] read(InputStream input) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      int oneByte;
      while ((oneByte = input.read()) != -1) {
        bytes.write(oneByte);
      }
      return bytes.toByteArray();
    } catch (IOException e) {
      throw unchecked(e);
    }
  }

  private static byte[] readBuffered(InputStream input, int size) {
    byte[] buffer = new byte[size];
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      int count;
      while ((count = input.read(buffer)) != -1) {
        bytes.write(buffer, 0, count);
      }
      return bytes.toByteArray();
    } catch (IOException e) {
      throw unchecked(e);
    }
  }
}

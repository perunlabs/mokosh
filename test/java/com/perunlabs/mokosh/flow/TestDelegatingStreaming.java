package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.flow.DelegatingStreaming.streaming;
import static java.lang.String.format;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.perunlabs.mokosh.MokoshException;
import com.perunlabs.mokosh.run.Running;

public class TestDelegatingStreaming {
  private InputStream input;
  private Running<Void> running;
  private Streaming streaming;
  private byte[] array;

  @Before
  public void before() {
    givenTest(this);
  }

  @Test
  public void delegates_await() {
    given(streaming = streaming(running, input));
    when(streaming.await());
    thenCalled(running).await();
  }

  @Test
  public void delegates_abort() {
    given(streaming = streaming(running, input));
    when(streaming.abort());
    thenCalled(running).abort();
  }

  @Test
  public void delegates_is_running() {
    given(streaming = streaming(running, input));
    given(willReturn(true), running).isRunning();
    when(streaming.isRunning());
    thenReturned(true);
    thenCalled(running).isRunning();
  }

  @Test
  public void delegates_read() throws IOException {
    given(streaming = streaming(running, input));
    when(streaming.read());
    thenCalled(input).read();
  }

  @Test
  public void delegates_read_array() throws IOException {
    given(streaming = streaming(running, input));
    when(streaming.read(array));
    thenCalled(input).read(array);
  }

  @Test
  public void delegates_read_subarray() throws IOException {
    given(streaming = streaming(running, input));
    when(streaming.read(array, 0, 1));
    thenCalled(input).read(array, 0, 1);
  }

  @Test
  public void delegates_skip() throws IOException {
    given(streaming = streaming(running, input));
    when(streaming.skip(3));
    thenCalled(input).skip(3);
  }

  @Test
  public void delegates_available() throws IOException {
    given(streaming = streaming(running, input));
    when(streaming.available());
    thenCalled(input).available();
  }

  @Test
  public void delegates_close() throws IOException {
    given(streaming = streaming(running, input));
    when(() -> streaming.close());
    thenCalled(input).close();
  }

  @Test
  public void delegates_mark() throws IOException {
    given(streaming = streaming(running, input));
    when(() -> streaming.mark(3));
    thenCalled(input).mark(3);
  }

  @Test
  public void delegates_reset() throws IOException {
    given(streaming = streaming(running, input));
    when(() -> streaming.reset());
    thenCalled(input).reset();
  }

  @Test
  public void delegates_mark_supported() throws IOException {
    given(streaming = streaming(running, input));
    when(() -> streaming.markSupported());
    thenCalled(input).markSupported();
  }

  @Test
  public void implements_to_string() {
    given(streaming = streaming(running, input));
    when(streaming.toString());
    thenReturned(format("streaming(%s, %s)", running, input));
  }

  @Test
  public void checks_that_running_is_not_null() {
    given(running = null);
    when(() -> streaming(running, input));
    thenThrown(MokoshException.class);
  }

  @Test
  public void checks_that_input_is_not_null() {
    given(input = null);
    when(() -> streaming(running, input));
    thenThrown(MokoshException.class);
  }
}

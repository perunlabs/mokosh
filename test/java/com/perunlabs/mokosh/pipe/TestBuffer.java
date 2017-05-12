package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.pipe.Buffer.buffer;
import static com.perunlabs.mokosh.run.Run.run;
import static com.perunlabs.mokosh.testing.Testing.bytes;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.sleepSeconds;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;
import com.perunlabs.mokosh.run.Running;

public class TestBuffer {
  @Rule
  public final Timeout timeout = seconds(1);

  private Buffer buffer;
  private byte oneByte;
  private byte[] bytes;

  private Running<?> writing, reading;

  @Before
  public void before() {
    givenTest(this);
    given(buffer = buffer(1));
    given(oneByte = 123);
  }

  @After
  public void after() {
    writing.abort();
    reading.abort();
  }

  @Test
  public void transfers_single_byte() {
    given(buffer = buffer(100));
    given(writing = run(() -> write(buffer.output(), oneByte)));
    given(reading = run(() -> read(buffer.input())));
    when(reading.await().get());
    thenReturned(new byte[] { oneByte });
  }

  @Test
  public void transfers_many_bytes_one_by_one() {
    given(buffer = buffer(1_000));
    given(bytes = bytes(1_000_000));
    given(writing = run(() -> write(buffer.output(), bytes)));
    given(reading = run(() -> read(buffer.input())));
    when(reading.await().get());
    thenReturned(bytes(1_000_000));
  }

  @Test
  public void transfers_many_bytes_buffered() {
    given(buffer = buffer(1_000_000));
    given(bytes = bytes(10_000_000));
    given(writing = run(() -> writeBuffered(buffer.output(), bytes)));
    given(reading = run(() -> readBuffered(buffer.input(), 10_000)));
    when(reading.await().get());
    thenReturned(bytes(10_000_000));
  }

  @Test
  public void reads_after_writer_is_dead() {
    given(buffer = buffer(100));
    given(writing = run(() -> write(buffer.output(), oneByte)));
    given(writing.await());
    given(reading = run(() -> read(buffer.input())));
    when(reading.await().get());
    thenReturned(new byte[] { oneByte });
  }

  @Test
  public void reader_can_connect_first() {
    given(buffer = buffer(100));
    given(reading = run(() -> read(buffer.input())));
    given(sleepSeconds(0.1));
    given(writing = run(() -> write(buffer.output(), oneByte)));
    when(reading.await().get());
    thenReturned(new byte[] { oneByte });
  }

  @Test
  public void aborts_reading_byte() {
    given(buffer = buffer(100));
    given(interruptMeAfterSeconds(0.1));
    when(() -> buffer.input().read());
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_reading_subarray() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> buffer.input().read(bytes, 0, 1));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_reading_array() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> buffer.input().read(bytes));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_reader_skipping() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> buffer.input().skip(1));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_writing_byte() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    given(() -> buffer.output().write(oneByte));
    when(() -> buffer.output().write(oneByte));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_writing_subarray() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    when(() -> buffer.output().write(bytes(5), 0, 2));
    thenThrown(AbortException.class);
  }

  @Test
  public void aborts_writing_array() {
    given(buffer = buffer(1));
    given(interruptMeAfterSeconds(0.1));
    given(() -> buffer.output().write(oneByte));
    when(() -> buffer.output().write(bytes(5)));
    thenThrown(AbortException.class);
  }

  @Test
  public void checks_that_size_is_positive() {
    when(() -> buffer(0));
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
      throw new UncheckedIOException(e);
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
      throw new UncheckedIOException(e);
    }
  }

  private static void write(OutputStream output, byte... bytes) {
    try {
      for (byte oneByte : bytes) {
        output.write(oneByte);
      }
      output.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeBuffered(OutputStream output, byte... bytes) {
    try {
      output.write(bytes);
      output.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

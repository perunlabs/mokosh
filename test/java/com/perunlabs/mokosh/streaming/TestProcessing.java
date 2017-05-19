package com.perunlabs.mokosh.streaming;

import static com.perunlabs.mokosh.streaming.Command.command;
import static com.perunlabs.mokosh.streaming.Processing.processing;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.readAllBytes;
import static com.perunlabs.mokosh.testing.Testing.withMessage;
import static java.lang.String.format;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.spy;
import static org.testory.Testory.then;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.thenCalledNever;
import static org.testory.Testory.thenEqual;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestProcessing {
  @Rule
  public final Timeout timeout = seconds(1);
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private String string;
  private Streaming processing;
  private Supplier<Void> result;
  private InputStream stdin;
  private ByteArrayOutputStream stderr;
  private File file;

  @Before
  public void before() {
    givenTest(this);
    given(stdin = new ByteArrayInputStream(new byte[0]));
    given(stderr = spy(new ByteArrayOutputStream()));
  }

  @Test
  public void reads_from_stdin_and_writes_to_stdout() throws IOException {
    given(stdin = new ByteArrayInputStream(encode(string)));
    given(processing = processing(command("tee").stdin(stdin)));
    when(readAllBytes(processing));
    thenReturned(encode(string));
  }

  @Test
  public void writes_to_stdout() throws IOException {
    given(processing = processing(command("echo", "-n", string)));
    when(readAllBytes(processing));
    thenReturned(encode(string));
    thenEqual(stderr.toByteArray(), new byte[0]);
  }

  @Test
  public void writes_to_stderr() throws IOException {
    given(processing = processing(command("cat", "abcdefg").stderr(stderr)));
    when(processing.await());
    thenEqual(readAllBytes(processing), new byte[0]);
    thenEqual(stderr.toByteArray(), encode("cat: abcdefg: No such file or directory\n"));
  }

  @Test
  public void std_streams_are_fast() throws IOException {
    given(stdin = new ByteArrayInputStream(new byte[1_000_000]));
    given(processing = processing(command("tee").stdin(stdin)));
    when(readAllBytes(processing));
    thenReturned(new byte[1_000_000]);
  }

  @Test
  public void stdout_is_unbuffered() throws IOException {
    given(file = folder.newFile());
    given(write(file.toPath(), encode("a")));
    given(processing = processing(command("tail", "-F", file.getAbsolutePath())));
    when(processing.read());
    thenReturned((int) 'a');
  }

  @Test
  public void getting_result_returns_null_if_process_completed() {
    given(processing = processing(command("echo", "-n", string)));
    when(() -> processing.await());
    thenReturned();
  }

  @Test
  public void getting_result_fails_if_process_failed_with_error_code() {
    given(processing = processing(command("cat", "abcdefg")));
    when(() -> processing.await().get());
    thenThrown();
  }

  @Test
  public void awaits_completion() {
    given(processing = processing(command("sleep", "0.1")));
    when(processing.await().get());
    thenReturned();
    then(!processing.isRunning());
  }

  @Test
  public void awaiting_is_abortable() {
    given(processing = processing(command("sleep", "1")));
    given(interruptMeAfterSeconds(0.1));
    when(() -> processing.await());
    thenThrown(AbortException.class);
  }

  @Test
  public void is_running_until_completion() {
    given(processing = processing(command("sleep", "0.1")));
    when(processing.isRunning());
    thenReturned(true);
  }

  @Test
  public void is_not_running_after_completion() {
    given(processing = processing(command("sleep", "0.1")));
    given(processing.await());
    when(processing.isRunning());
    thenReturned(false);
  }

  @Test
  public void aborts_running() {
    given(processing = processing(command("sleep", "0.1")));
    given(result = processing.abort().await());
    when(() -> result.get());
    thenThrown(RuntimeException.class);
    thenThrown(withMessage(equalTo("exit status = 143")));
  }

  @Test
  public void aborts_streaming() {
    given(processing = processing(command("sleep", "0.1")));
    given(result = processing.abort().await());
    when(() -> readAllBytes(processing));
    thenThrown(instanceOf(IOException.class));
    thenThrown(withMessage(equalTo("Stream closed")));
  }

  @Test
  public void aborting_has_no_effect_if_not_running() {
    given(processing = processing(command("echo", "-n", string)));
    given(processing.await());
    when(processing.abort().await());
    thenEqual(processing.await().get(), null);
    then(!processing.isRunning());
  }

  @Test
  public void abort_returns_this() {
    given(processing = processing(command("sleep", "0.1")));
    when(processing.abort());
    thenReturned(processing);
  }

  @Test
  public void broken_stdin_does_not_close_error() throws IOException {
    given(stdin = new InputStream() {
      public int read() throws IOException {
        throw new RuntimeException();
      }
    });
    given(processing = processing(command("tee").stdin(stdin).stderr(stderr)));
    when(processing.await());
    thenCalledNever(stderr).close();
  }

  @Test
  public void closes_stdin() throws IOException {
    given(stdin = spy(new ByteArrayInputStream(new byte[0])));
    given(processing = processing(command("echo", "-n", string).stdin(stdin)));
    when(processing.await());
    thenCalled(stdin).close();
  }

  @Test
  public void closes_stderr() throws IOException {
    given(processing = processing(command("echo", "-n", string).stderr(stderr)));
    when(processing.await());
    thenCalled(stderr).close();
  }

  @Test
  public void implements_to_string() {
    given(processing = processing(command("echo", "-n", string)));
    when(processing.toString());
    thenReturned(format("processing(echo -n %s)", string));
  }

  @Test
  public void null_checks_command() {
    when(() -> processing(null));
    thenThrown(MokoshException.class);
  }

  private static byte[] encode(String string) {
    return string.getBytes(Charset.defaultCharset());
  }
}

package com.perunlabs.mokosh.run;

import static com.perunlabs.mokosh.run.RunningProcess.run;
import static com.perunlabs.mokosh.testing.Testing.interruptMeAfterSeconds;
import static com.perunlabs.mokosh.testing.Testing.withMessage;
import static org.hamcrest.Matchers.equalTo;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.perunlabs.mokosh.AbortException;
import com.perunlabs.mokosh.MokoshException;

public class TestRunningProcess {
  @Rule
  public final Timeout timeout = seconds(1);

  private String string;
  private Running<Void> running;
  private Supplier<Void> result;
  private InputStream stdin;
  private OutputStream stdout, stderr;
  private ByteArrayOutputStream buffer;
  private ProcessBuilder processBuilder;

  @Before
  public void before() {
    given(processBuilder = new ProcessBuilder("tee"));
    givenTest(this);
    given(stdin = new ByteArrayInputStream(new byte[0]));
    given(buffer = new ByteArrayOutputStream());
  }

  @Test
  public void writes_to_output_stream() {
    given(stdout = buffer);
    given(running = run(new ProcessBuilder("echo", "-n", string), stdin, stdout, stderr));
    when(running.await().get());
    thenReturned();
    thenEqual(buffer.toByteArray(), encode(string));
  }

  @Test
  public void writes_to_error_stream() {
    given(stdin = new ByteArrayInputStream(new byte[0]));
    given(stderr = buffer);
    given(running = run(new ProcessBuilder("cat", "abcdefg"), stdin, stdout, stderr));
    when(() -> running.await().get());
    thenThrown();
    thenEqual(buffer.toByteArray(), encode("cat: abcdefg: No such file or directory\n"));
  }

  @Test
  public void reads_from_input_stream() {
    given(stdout = buffer);
    given(stdin = new ByteArrayInputStream(encode(string)));
    given(running = run(new ProcessBuilder("tee"), stdin, stdout, stderr));
    when(running.await().get());
    thenReturned();
    thenEqual(buffer.toByteArray(), encode(string));
  }

  @Test
  public void awaits_completion() {
    given(running = run(new ProcessBuilder("sleep", "0.1"), stdin, stdout, stderr));
    when(running.await().get());
    thenReturned();
    then(!running.isRunning());
  }

  @Test
  public void awaiting_is_abortable() {
    given(running = run(new ProcessBuilder("sleep", "1"), stdin, stdout, stderr));
    given(interruptMeAfterSeconds(0.1));
    when(() -> running.await());
    thenThrown(AbortException.class);
  }

  @Test
  public void is_running_until_completion() {
    given(running = run(new ProcessBuilder("sleep", "0.1"), stdin, stdout, stderr));
    when(running.isRunning());
    thenReturned(true);
  }

  @Test
  public void is_not_running_after_completion() {
    given(running = run(new ProcessBuilder("sleep", "0.1"), stdin, stdout, stderr));
    given(running.await());
    when(running.isRunning());
    thenReturned(false);
  }

  @Test
  public void aborts() {
    given(running = run(new ProcessBuilder("sleep", "0.1"), stdin, stdout, stderr));
    given(result = running.abort().await());
    when(() -> result.get());
    thenThrown(RuntimeException.class);
    thenThrown(withMessage(equalTo("exit status = 143")));
  }

  @Test
  public void aborting_has_no_effect_if_not_running() {
    given(stdout = buffer);
    given(running = run(new ProcessBuilder("echo", "-n", string), stdin, stdout, stderr));
    given(running.await());
    when(running.abort().await().get());
    thenEqual(buffer.toByteArray(), encode(string));
    then(!running.isRunning());
  }

  @Test
  public void abort_returns_this() {
    given(running = run(new ProcessBuilder("sleep", "0.1"), stdin, stdout, stderr));
    when(running.abort());
    thenReturned(running);
  }

  @Test
  public void broken_stdin_breaks_output_and_error() throws IOException {
    given(stdin = new InputStream() {
      public int read() throws IOException {
        throw new RuntimeException();
      }
    });
    given(running = run(new ProcessBuilder("tee"), stdin, stdout, stderr));
    when(running.await());
    thenCalledNever(stdout).close();
    thenCalledNever(stderr).close();
  }

  @Test
  public void closes_stdin() throws IOException {
    given(stdin = spy(new ByteArrayInputStream(new byte[0])));
    given(running = run(new ProcessBuilder("echo", "-n", string), stdin, stdout, stderr));
    when(running.await());
    thenCalled(stdin).close();
  }

  @Test
  public void closes_stdout() throws IOException {
    given(running = run(new ProcessBuilder("echo", "-n", string), stdin, stdout, stderr));
    when(running.await());
    thenCalled(stdout).close();
  }

  @Test
  public void closes_stderr() throws IOException {
    given(running = run(new ProcessBuilder("echo", "-n", string), stdin, stdout, stderr));
    when(running.await());
    thenCalled(stdout).close();
  }

  @Test
  public void null_checks_process_builder() {
    given(processBuilder = null);
    when(() -> run(processBuilder, stdin, stdout, stderr));
    thenThrown(MokoshException.class);
  }

  @Test
  public void null_checks_process_stdin() {
    given(stdin = null);
    when(() -> run(processBuilder, stdin, stdout, stderr));
    thenThrown(MokoshException.class);
  }

  @Test
  public void null_checks_process_stdout() {
    given(stdout = null);
    when(() -> run(processBuilder, stdin, stdout, stderr));
    thenThrown(MokoshException.class);
  }

  @Test
  public void null_checks_process_stderr() {
    given(stderr = null);
    when(() -> run(processBuilder, stdin, stdout, stderr));
    thenThrown(MokoshException.class);
  }

  private static byte[] encode(String string) {
    return string.getBytes(Charset.defaultCharset());
  }
}

package com.perunlabs.mokosh;

import static com.perunlabs.mokosh.AbortException.abortIfInterrupted;
import static com.perunlabs.mokosh.run.Run.run;
import static org.testory.Testory.given;
import static org.testory.Testory.thenReturned;
import static org.testory.Testory.thenThrown;
import static org.testory.Testory.when;

import java.util.function.Supplier;

import org.junit.Test;

import com.perunlabs.mokosh.run.Running;

public class TestAbortException {
  private Running<Void> running;
  private Supplier<Void> result;

  @Test
  public void aborts_if_interrupted() {
    given(running = run(() -> {
      Thread.currentThread().interrupt();
      abortIfInterrupted();
    }));
    given(result = running.await());
    when(() -> result.get());
    thenThrown(AbortException.class);
  }

  @Test
  public void does_not_abort_if_not_interrupted() {
    when(() -> abortIfInterrupted());
    thenReturned();
  }
}

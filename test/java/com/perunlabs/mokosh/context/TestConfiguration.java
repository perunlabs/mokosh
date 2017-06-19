package com.perunlabs.mokosh.context;

import static com.perunlabs.mokosh.context.Configuration.configure;
import static com.perunlabs.mokosh.running.Supplying.supplying;
import static org.junit.rules.Timeout.seconds;
import static org.testory.Testory.any;
import static org.testory.Testory.given;
import static org.testory.Testory.givenTest;
import static org.testory.Testory.thenCalled;
import static org.testory.Testory.when;
import static org.testory.Testory.willReturn;

import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class TestConfiguration {
  @Rule
  public final Timeout timeout = seconds(1);

  private ThreadFactory factory;
  private Runnable runnable;
  private Thread thread;

  @Before
  public void before() {
    givenTest(this);
  }

  @Test
  public void supplying_uses_thread_factory() {
    Configuration.;
    given(willReturn(thread), factory).newThread(any(Runnable.class));
    when(supplying(runnable));
    thenCalled(thread).start();
  }
}

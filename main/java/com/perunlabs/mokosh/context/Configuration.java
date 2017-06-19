package com.perunlabs.mokosh.context;

import static com.perunlabs.mokosh.MokoshException.check;

import java.util.concurrent.ThreadFactory;

public class Configuration {
  private static volatile Configuration configuration = new Configuration()
      .set(runnable -> new Thread(runnable));

  private ThreadFactory threadFactory = runnable -> new Thread(runnable);

  public Configuration set(ThreadFactory threadFactory) {
    check(threadFactory != null);
    this.threadFactory = threadFactory;
    return this;
  }
}

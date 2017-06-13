package com.perunlabs.mokosh.context;

import java.util.function.Supplier;

public class InheritableContext implements Context {
  private final Context context;

  private InheritableContext(Context context) {
    this.context = context;
  }

  public static Context inheritable(Context context) {
    return new InheritableContext(context);
  }

  public <T> Supplier<T> wrap(Supplier<T> supplier) {
    return context.wrap(supplier);
  }
}

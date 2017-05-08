package com.perunlabs.mokosh.common;

import java.util.function.Supplier;

public class Lambdas {
  public static Supplier<Void> asSupplier(Runnable runnable) {
    return () -> {
      runnable.run();
      return null;
    };
  }

  public static boolean failed(Supplier<?> supplier) {
    try {
      supplier.get();
    } catch (Throwable e) {
      return true;
    }
    return false;
  }
}

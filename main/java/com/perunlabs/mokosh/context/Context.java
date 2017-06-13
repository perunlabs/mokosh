package com.perunlabs.mokosh.context;

import java.util.function.Supplier;

public interface Context {
  <T> Supplier<T> wrap(Supplier<T> supplier);
}

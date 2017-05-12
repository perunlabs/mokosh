package com.perunlabs.mokosh.pipe;

import java.util.Iterator;

public interface Output<T> {
  /**
   * Iterates over input and sends objects through pipe. Then it closes the pipe.
   */
  void connect(Iterator<? extends T> input);
}

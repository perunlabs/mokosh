package com.perunlabs.mokosh.pipe;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;

public class Buffer {
  private final InputStream input;
  private final OutputStream output;

  private Buffer(InputStream input, OutputStream output) {
    this.input = input;
    this.output = output;
  }

  public static Buffer buffer(int size) {
    check(size > 0);
    try {
      PipedInputStream input = new PipedInputStream(size);
      PipedOutputStream output = new PipedOutputStream(input);
      return new Buffer(unchecked(input), unchecked(output));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public OutputStream output() {
    return output;
  }

  public InputStream input() {
    return input;
  }
}

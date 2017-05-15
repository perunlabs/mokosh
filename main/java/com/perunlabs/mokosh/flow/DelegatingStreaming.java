package com.perunlabs.mokosh.flow;

import static com.perunlabs.mokosh.MokoshException.check;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import com.perunlabs.mokosh.run.Running;

public class DelegatingStreaming extends Streaming {
  private final Running<Void> running;
  private final InputStream input;

  private DelegatingStreaming(Running<Void> running, InputStream input) {
    this.running = running;
    this.input = input;
  }

  public static Streaming streaming(Running<Void> running, InputStream input) {
    check(running != null);
    check(input != null);
    return new DelegatingStreaming(running, input);
  }

  public Supplier<Void> await() {
    return running.await();
  }

  public Running<Void> abort() {
    return running.abort();
  }

  public boolean isRunning() {
    return running.isRunning();
  }

  public int read() throws IOException {
    return input.read();
  }

  public int read(byte[] b) throws IOException {
    return input.read(b);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    return input.read(b, off, len);
  }

  public long skip(long n) throws IOException {
    return input.skip(n);
  }

  public int available() throws IOException {
    return input.available();
  }

  public void close() throws IOException {
    input.close();
  }

  public synchronized void mark(int readlimit) {
    input.mark(readlimit);
  }

  public synchronized void reset() throws IOException {
    input.reset();
  }

  public boolean markSupported() {
    return input.markSupported();
  }

  public String toString() {
    return format("streaming(%s, %s)", running, input);
  }
}

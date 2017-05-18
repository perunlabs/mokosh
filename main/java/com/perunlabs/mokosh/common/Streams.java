package com.perunlabs.mokosh.common;

import static com.perunlabs.mokosh.AbortException.abortIfInterrupted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
  public static void pump(InputStream input, OutputStream output) throws IOException {
    int oneByte;
    while ((oneByte = input.read()) != -1) {
      output.write(oneByte);
      abortIfInterrupted();
    }
    output.flush();
  }
}

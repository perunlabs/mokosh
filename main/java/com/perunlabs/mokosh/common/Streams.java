package com.perunlabs.mokosh.common;

import static com.perunlabs.mokosh.AbortException.abortIfInterrupted;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
  public static void pump(InputStream input, OutputStream output) {
    try {
      int count;
      byte[] buffer = new byte[8 * 1024];
      while ((count = input.read(buffer)) != -1) {
        abortIfInterrupted();
        output.write(buffer, 0, count);
      }
    } catch (IOException e) {
      throw unchecked(e);
    }
  }
}

package com.perunlabs.mokosh.pipeline;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Collections.add;
import static com.perunlabs.mokosh.common.Streams.pump;
import static com.perunlabs.mokosh.common.Unchecked.unchecked;
import static com.perunlabs.mokosh.pipeline.Util.runAll;
import static com.perunlabs.mokosh.run.EntangledRunning.entangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.BiConsumer;

import com.perunlabs.mokosh.pipe.Buffer;
import com.perunlabs.mokosh.pipe.Output;
import com.perunlabs.mokosh.pipe.Pipe;
import com.perunlabs.mokosh.run.Run;
import com.perunlabs.mokosh.run.Running;

public class StreamPipeline {
  private final List<Runnable> runnables;
  private final InputStream exit;

  StreamPipeline(InputStream exit, List<Runnable> runnables) {
    this.exit = exit;
    this.runnables = runnables;
  }

  public <T> ObjectPipeline<T> map(BiConsumer<InputStream, Output<T>> code, Pipe pipe) {
    check(code != null);
    check(pipe != null);
    Runnable runnable = () -> code.accept(exit, pipe.output());
    return new ObjectPipeline<T>(pipe.input(), add(runnable, runnables));
  }

  public StreamPipeline map(BiConsumer<InputStream, OutputStream> code, Buffer buffer) {
    check(code != null);
    check(buffer != null);
    Runnable runnable = () -> code.accept(exit, buffer.output());
    return new StreamPipeline(buffer.input(), add(runnable, runnables));
  }

  public Running<byte[]> run() {
    List<Running<Void>> runnings = runAll(runnables);
    Running<byte[]> collecting = Run.run(() -> {
      ByteArrayOutputStream array = new ByteArrayOutputStream();
      pump(exit, array);
      return array.toByteArray();
    });
    return entangle(collecting, runnings.toArray(new Running[0]));
  }

  public Running<Void> run(OutputStream output) {
    List<Running<Void>> runnings = runAll(runnables);
    Running<Void> outputting = Run.run(() -> {
      try {
        pump(exit, output);
        output.close();
      } catch (IOException e) {
        throw unchecked(e);
      }
    });
    return entangle(outputting, runnings.toArray(new Running[0]));
  }
}

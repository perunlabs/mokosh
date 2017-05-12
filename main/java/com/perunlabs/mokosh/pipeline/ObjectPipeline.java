package com.perunlabs.mokosh.pipeline;

import static com.perunlabs.mokosh.MokoshException.check;
import static com.perunlabs.mokosh.common.Collections.add;
import static com.perunlabs.mokosh.common.Collections.collectToList;
import static com.perunlabs.mokosh.pipeline.Util.runAll;
import static com.perunlabs.mokosh.run.EntangledRunning.entangle;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.perunlabs.mokosh.pipe.Buffer;
import com.perunlabs.mokosh.pipe.Output;
import com.perunlabs.mokosh.pipe.Pipe;
import com.perunlabs.mokosh.run.Run;
import com.perunlabs.mokosh.run.Running;

public class ObjectPipeline<T> {
  private final List<Runnable> runnables;
  private final Iterator<T> exit;

  ObjectPipeline(Iterator<T> exit, List<Runnable> runnables) {
    this.exit = exit;
    this.runnables = runnables;
  }

  public <S> ObjectPipeline<S> map(Function<T, S> code, Pipe<S> pipe) {
    check(code != null);
    check(pipe != null);
    Runnable runnable = () -> {
      pipe.output().connect(new Iterator<S>() {
        public boolean hasNext() {
          return exit.hasNext();
        }

        public S next() {
          return code.apply(exit.next());
        }
      });
    };
    return new ObjectPipeline<>(pipe.input(), add(runnable, runnables));
  }

  public StreamPipeline map(BiConsumer<Iterator<T>, OutputStream> code, Buffer buffer) {
    check(code != null);
    check(buffer != null);
    Runnable runnable = () -> code.accept(exit, buffer.output());
    return new StreamPipeline(buffer.input(), add(runnable, runnables));
  }

  public Running<List<T>> run() {
    List<Running<Void>> runnings = runAll(runnables);
    Running<List<T>> collecting = Run.run(() -> collectToList(exit));
    return entangle(collecting, runnings.toArray(new Running[0]));
  }

  public Running<Void> run(Output output) {
    List<Running<Void>> runnings = runAll(runnables);
    Running<Void> outputting = Run.run(() -> output.connect(exit));
    return entangle(outputting, runnings.toArray(new Running[0]));
  }
}

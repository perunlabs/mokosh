package com.perunlabs.mokosh.pipeline;

import static com.perunlabs.mokosh.MokoshException.check;
import static java.util.Arrays.asList;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import com.perunlabs.mokosh.pipe.Buffer;
import com.perunlabs.mokosh.pipe.Output;
import com.perunlabs.mokosh.pipe.Pipe;

public class Pipeline {
  public static <T> ObjectPipeline<T> pipeline(Iterator<T> entrance) {
    check(entrance != null);
    return new ObjectPipeline<T>(entrance, new ArrayList<>());
  }

  public static <T> ObjectPipeline<T> pipeline(Consumer<Output<T>> entrance, Pipe<T> pipe) {
    check(entrance != null);
    check(pipe != null);
    Runnable runnable = () -> entrance.accept(pipe.output());
    return new ObjectPipeline<T>(pipe.input(), asList(runnable));
  }

  public static StreamPipeline pipeline(InputStream entrance) {
    check(entrance != null);
    return new StreamPipeline(entrance, new ArrayList<>());
  }

  public static StreamPipeline pipeline(Consumer<OutputStream> entrance, Buffer buffer) {
    check(entrance != null);
    check(buffer != null);
    Runnable runnable = () -> entrance.accept(buffer.output());
    return new StreamPipeline(buffer.input(), asList(runnable));
  }
}

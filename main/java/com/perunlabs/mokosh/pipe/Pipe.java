package com.perunlabs.mokosh.pipe;

import java.util.Iterator;

/**
 * Allows transfer of objects between threads. By default it should be used by two threads. One
 * thread uses output to send objects. Other thread uses input to receive objects that went through
 * this pipe.
 *
 * Output thread may decide that no more objects will be sent by closing this pipe's output. Input
 * thread notices it through input iterator.
 *
 * Output thread may block if pipe is full. Input thread may block if no objects are available.
 *
 * Some implementations may offer different features: support more input or output threads,
 * different blocking policies.
 *
 * @param <T>
 *          type of transferred objects
 */
public interface Pipe<T> {
  Iterator<T> input();

  Output<T> output();
}

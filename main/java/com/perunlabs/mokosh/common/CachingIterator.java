package com.perunlabs.mokosh.common;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.Optional;

public class CachingIterator<E> implements Iterator<E> {
  private final Iterator<E> iterator;
  private Optional<Boolean> hasNext = Optional.empty();

  private CachingIterator(Iterator<E> iterator) {
    this.iterator = iterator;
  }

  public static <E> Iterator<E> caching(Iterator<E> iterator) {
    return new CachingIterator(requireNonNull(iterator));
  }

  public synchronized boolean hasNext() {
    if (!hasNext.isPresent()) {
      hasNext = Optional.of(iterator.hasNext());
    }
    return hasNext.get();
  }

  public synchronized E next() {
    E next = iterator.next();
    hasNext = Optional.empty();
    return next;
  }

  public String toString() {
    return format("caching(%s)", iterator);
  }
}

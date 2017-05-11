package com.perunlabs.mokosh.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Collections {
  public static <E> List<E> add(E element, List<E> list) {
    ArrayList<E> copy = new ArrayList<>(list);
    copy.add(element);
    return copy;
  }

  public static <E> List<E> collectToList(Iterator<E> iterator) {
    List<E> list = new ArrayList<>();
    iterator.forEachRemaining(list::add);
    return list;
  }
}

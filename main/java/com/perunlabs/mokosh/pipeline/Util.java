package com.perunlabs.mokosh.pipeline;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import com.perunlabs.mokosh.run.Run;
import com.perunlabs.mokosh.run.Running;

public class Util {
  static List<Running<Void>> runAll(Collection<Runnable> runnables) {
    return runnables.stream()
        .map(Run::run)
        .collect(toList());
  }
}

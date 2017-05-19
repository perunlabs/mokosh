package com.perunlabs.mokosh;

import static java.util.Arrays.asList;

import com.perunlabs.mokosh.iterating.Replicator;
import com.perunlabs.mokosh.running.Entangling;
import com.perunlabs.mokosh.running.Supplying;
import com.perunlabs.mokosh.streaming.Processing;

class Build {
  public static Object run = asList(
      Supplying.class,
      Entangling.class);

  public static Object iterating = asList(
      Replicator.class,
      com.perunlabs.mokosh.iterating.Working.class,
      com.perunlabs.mokosh.iterating.Buffering.class,
      com.perunlabs.mokosh.iterating.Delegating.class);

  public static Object streaming = asList(
      Processing.class,
      com.perunlabs.mokosh.streaming.Buffering.class,
      com.perunlabs.mokosh.streaming.Delegating.class);
}

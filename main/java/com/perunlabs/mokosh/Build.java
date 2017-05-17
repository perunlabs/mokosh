package com.perunlabs.mokosh;

import static java.util.Arrays.asList;

import com.perunlabs.mokosh.flow.BufferingIterating;
import com.perunlabs.mokosh.flow.BufferingStreaming;
import com.perunlabs.mokosh.flow.DelegatingIterating;
import com.perunlabs.mokosh.flow.DelegatingStreaming;
import com.perunlabs.mokosh.flow.Replicator;
import com.perunlabs.mokosh.run.EntangledRunning;
import com.perunlabs.mokosh.run.Run;
import com.perunlabs.mokosh.run.RunningProcess;

class Build {
  public static Object run = asList(
      Run.class,
      RunningProcess.class,
      EntangledRunning.class);
  public static Object flow = asList(
      BufferingStreaming.class,
      BufferingIterating.class,
      Replicator.class,
      DelegatingIterating.class,
      DelegatingStreaming.class);
}

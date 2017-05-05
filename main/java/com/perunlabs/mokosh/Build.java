package com.perunlabs.mokosh;

import static java.util.Arrays.asList;

import com.perunlabs.mokosh.run.EntangledRunning;
import com.perunlabs.mokosh.run.Run;
import com.perunlabs.mokosh.run.RunningProcess;

class Build {
  public static Object run = asList(
      Run.class,
      RunningProcess.class,
      EntangledRunning.class);
}

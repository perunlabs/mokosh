package com.perunlabs.mokosh.streaming;

import static com.perunlabs.mokosh.MokoshException.check;
import static java.util.Arrays.asList;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Command {
  public final List<String> command;
  public final Optional<InputStream> stdin;
  public final Optional<OutputStream> stderr;

  private Command(List<String> command, Optional<InputStream> stdin, Optional<OutputStream> stderr) {
    this.command = new ArrayList<>(command);
    this.stdin = stdin;
    this.stderr = stderr;
  }

  public static Command command(List<String> command) {
    check(command != null);
    List<String> commandCopy = new ArrayList<>(command);
    check(!commandCopy.contains(null));
    return new Command(command, Optional.empty(), Optional.empty());
  }

  public static Command command(String... command) {
    check(command != null);
    return command(asList(command));
  }

  public Command stdin(InputStream stdin) {
    check(!this.stdin.isPresent());
    return new Command(command, Optional.of(stdin), stderr);
  }

  public Command stderr(OutputStream stderr) {
    check(!this.stderr.isPresent());
    return new Command(command, stdin, Optional.of(stderr));
  }
}

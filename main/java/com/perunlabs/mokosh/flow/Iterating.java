package com.perunlabs.mokosh.flow;

import java.util.Iterator;

import com.perunlabs.mokosh.run.Running;

public interface Iterating<T> extends Running<Void>, Iterator<T> {}

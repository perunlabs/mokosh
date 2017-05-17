package com.perunlabs.mokosh.iterating;

import java.util.Iterator;

import com.perunlabs.mokosh.running.Running;

public interface Iterating<T> extends Running<Void>, Iterator<T> {}

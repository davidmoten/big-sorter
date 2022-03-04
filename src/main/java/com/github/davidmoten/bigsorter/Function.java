package com.github.davidmoten.bigsorter;

@FunctionalInterface
public interface Function<S, T> {

    T apply(S s) throws Exception;
    
}

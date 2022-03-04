package com.github.davidmoten.bigsorter;

@FunctionalInterface
public interface BiConsumer<S, T> {

    void accept(S s, T t) throws Exception;
    
}

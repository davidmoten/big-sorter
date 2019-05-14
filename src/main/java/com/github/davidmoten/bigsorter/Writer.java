package com.github.davidmoten.bigsorter;

import java.io.IOException;

public interface Writer<T> {

    void write(T value) throws IOException;
    
}

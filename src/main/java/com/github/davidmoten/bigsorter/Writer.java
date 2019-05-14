package com.github.davidmoten.bigsorter;

import java.io.Closeable;
import java.io.IOException;

public interface Writer<T> extends Closeable {

    void write(T value) throws IOException;
    
}

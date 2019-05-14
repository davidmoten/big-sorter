package com.github.davidmoten.bigsorter;

import java.io.IOException;

public interface Reader<T> {

    T read() throws IOException;
    
}

package com.github.davidmoten.bigsorter;

import java.io.Closeable;
import java.io.IOException;

public interface Reader<T> extends Closeable {

    T read() throws IOException;

    default T readAutoClosing() throws IOException {
        T v = read();
        if (v == null) {
            close();
        }
        return v;
    }

}

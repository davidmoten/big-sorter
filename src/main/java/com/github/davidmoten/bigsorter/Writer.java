package com.github.davidmoten.bigsorter;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;

public interface Writer<T> extends Closeable {

    void write(T value) throws IOException;
    
    void flush() throws IOException;
    
    default <S> Writer<S> map(Function<? super S, ? extends T> mapper) {
        Writer<T> w = this;
        return new Writer<S>() {

            @Override
            public void write(S value) throws IOException {
                w.write(mapper.apply(value));
            }

            @Override
            public void flush() throws IOException {
                w.close();
            }

            @Override
            public void close() throws IOException {
                w.close();
            }
        };
    }
    
}

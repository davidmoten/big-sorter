package com.github.davidmoten.bigsorter.internal;

import java.io.IOException;
import java.util.Iterator;

import com.github.davidmoten.bigsorter.Reader;
import com.github.davidmoten.guavamini.Preconditions;

public final class ReaderFromIterator<T> implements Reader<T> {

    private Iterator<? extends T> it;

    public ReaderFromIterator(Iterator<? extends T> it) {
        Preconditions.checkNotNull(it);
        this.it = it;
    }

    @Override
    public T read() throws IOException {
        if (it == null || !it.hasNext()) {
            // help gc
            it = null;
            return null;
        } else {
            return it.next();
        }
    }
    
    @Override
    public void close() throws IOException {
        // do nothing
    }

}

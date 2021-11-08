package com.github.davidmoten.bigsorter.internal;

import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ReaderFromIteratorTest {

    @Test
    public void test() throws IOException {
        ReaderFromIterator<String> r = new ReaderFromIterator<String>(new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }
        });
        assertNull(r.read());
        assertNull(r.read());
    }
    
}

package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ReaderTest {

    private static final Reader<Integer> EMPTY_READER = new Reader<Integer>() {

        @Override
        public void close() throws IOException {
            // do nothing
        }

        @Override
        public Integer read() throws IOException {
            return null;
        }
    };
    
    private static final Reader<Integer> THROWS = new Reader<Integer>() {

        @Override
        public void close() throws IOException {
            // do nothing
        }

        @Override
        public Integer read() throws IOException {
            throw new IOException("boo");
        }
    };

    @Test
    public void testToIteratorEmpty() {
        assertFalse(EMPTY_READER.iterator().hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void testToIteratorEmptyNextThrows() {
        EMPTY_READER.iterator().next();
    }
    
    @Test(expected=UncheckedIOException.class)
    public void testToIteratorWhenReadThrows() {
        THROWS.iterator().next();
    }

}

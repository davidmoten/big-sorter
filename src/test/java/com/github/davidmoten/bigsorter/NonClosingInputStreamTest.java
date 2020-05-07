package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class NonClosingInputStreamTest {

    @Test
    public void test() throws IOException {
        // class is trivial, just ensure we get coverage without proper assertions
        boolean[] closed = new boolean[1];
        InputStream in = new InputStream() {

            @Override
            public int read() throws IOException {
                return 1;
            }

            @Override
            public void close() throws IOException {
                closed[0] = true;
            }

            @Override
            public synchronized void reset() throws IOException {
                // do nothing
            }
            
        };
        NonClosingInputStream s = new NonClosingInputStream(in);
        s.close();
        assertFalse(closed[0]);
        s.available();
        s.mark(1);
        s.markSupported();
        s.read();
        s.read(new byte[3]);
        s.read(new byte[3], 0, 2);
        s.reset();
        s.skip(1);
    }

}

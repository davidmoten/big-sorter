package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.io.OutputStream;

final class TestingOutputStream extends OutputStream {

    boolean flushed;

    @Override
    public void write(int b) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void flush() throws IOException {
        flushed = true;
    }

}

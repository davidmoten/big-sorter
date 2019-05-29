package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.junit.Test;

public class JavaSerializerTest {

    @Test(expected=RuntimeException.class)
    public void test() throws IOException {
        try (InputStream in = JavaSerializerTest.class.getResourceAsStream("/oo")) {
            Reader<Serializable> r = Serializer.java().createReader(in);
            r.read();
        }
    }


}

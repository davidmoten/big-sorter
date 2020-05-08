package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertTrue;

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
    
    @Test
    public void testFlushed() throws IOException {
        TestingOutputStream out = new TestingOutputStream();
        Serializer<Serializable> s = Serializer.java();
        try (Writer<Serializable> w  = s.createWriter(out)) {
            w.flush();
        }
        assertTrue(out.flushed);
    }
    
}

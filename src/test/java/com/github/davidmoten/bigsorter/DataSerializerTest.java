package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

public class DataSerializerTest {

    @Test
    public void testFlush() throws IOException {
        DataSerializer<String> s = new DataSerializer<String>() {

            @Override
            public String read(DataInputStream dis) throws IOException {
                return "hello";
            }

            @Override
            public void write(DataOutputStream dos, String value) throws IOException {

            }
        };
        TestingOutputStream out = new TestingOutputStream();
        try (Writer<String> w = s.createWriter(out)) {
            w.flush();
        }
        assertTrue(out.flushed);
    }

}

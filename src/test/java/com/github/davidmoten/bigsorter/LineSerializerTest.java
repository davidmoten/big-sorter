package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class LineSerializerTest {

    @Test
    public void testLF() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer<String> w = Serializer.linesUtf8().createWriter(out)) {
            w.write("hello");
        }
        assertEquals("hello\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testCRLF() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer<String> w = Serializer.linesUtf8(LineDelimiter.CARRIAGE_RETURN_LINE_FEED)
                .createWriter(out)) {
            w.write("hello");
        }
        assertEquals("hello\r\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testCharsetLF() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer<String> w = Serializer.lines(StandardCharsets.UTF_8, LineDelimiter.LINE_FEED)
                .createWriter(out)) {
            w.write("hello");
        }
        assertEquals("hello\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testCharsetCRLF() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer<String> w = Serializer
                .lines(StandardCharsets.UTF_8, LineDelimiter.CARRIAGE_RETURN_LINE_FEED)
                .createWriter(out)) {
            w.write("hello");
        }
        assertEquals("hello\r\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFlush() throws IOException {
        TestingOutputStream out = new TestingOutputStream();
        try (Writer<String> w = Serializer.lines(StandardCharsets.UTF_8, LineDelimiter.LINE_FEED)
                .createWriter(out)) {
            w.flush();
        }
        assertTrue(out.flushed);
    }
    
}

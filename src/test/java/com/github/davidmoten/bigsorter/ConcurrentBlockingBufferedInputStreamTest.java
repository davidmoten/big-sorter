package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

class ConcurrentBlockingBufferedInputStreamTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void testSmall() throws IOException {
        String s = "abcdefg";

        try (java.io.Reader r = new InputStreamReader( //
                new ConcurrentBlockingBufferedInputStream( //
                        new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), 3, executor))) {
            int ch = 0;
            List<Integer> chars = new ArrayList<>();
            while ((ch = r.read()) != -1) {
                chars.add(ch);
            }
            assertEquals(Arrays.asList(97, 98, 99, 100, 101, 102, 103), chars);
        }
    }

    @Test
    public void testByteBufferRead() {
        byte[] bytes = new byte[] { 1, 2, 3, 4 };
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        assertTrue(bb.hasRemaining());
    }

}

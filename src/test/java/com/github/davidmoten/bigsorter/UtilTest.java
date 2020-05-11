package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class UtilTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Util.class);
    }

    @Test
    public void testSameLinesTwoSortedFiles() throws IOException {
        File a = write("target/a", "12\n23\n34");
        File b = write("target/b", "12\n22\n34\n40");
        Comparator<String> comparator = Comparator.naturalOrder();
        Serializer<String> ser = Serializer.linesUtf8();
        File c = new File("target/c");
        Util.findSame(a, b, ser, comparator, c);
        assertEquals("12\n34\n", (new String(Files.readAllBytes(c.toPath()))));

        // assert same if A and B swapped
        Util.findSame(b, a, ser, comparator, c);
        assertEquals("12\n34\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    @Test
    public void testDifferentLinesTwoSortedFiles() throws IOException {
        File a = write("target/a", "12\n23\n34");
        File b = write("target/b", "12\n22\n34\n40");
        Comparator<String> comparator = Comparator.naturalOrder();
        Serializer<String> ser = Serializer.linesUtf8();
        File c = new File("target/c");
        Util.findDifferent(a, b, ser, comparator, c);
        assertEquals("22\n23\n40\n", (new String(Files.readAllBytes(c.toPath()))));

        // swap files
        Util.findDifferent(b, a, ser, comparator, c);
        assertEquals("22\n23\n40\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    @Test
    public void testComplementLinesTwoSortedFiles() throws IOException {
        File a = write("target/a", "12\n23\n34");
        File b = write("target/b", "12\n22\n34\n40");
        File c = new File("target/c");
        Util.findComplement(a, b, Serializer.linesUtf8(), Comparator.naturalOrder(), c);
        assertEquals("23\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    @Test
    public void testComplementLinesTwoSortedFilesSwapped() throws IOException {
        File a = write("target/a", "12\n23\n34");
        File b = write("target/b", "12\n22\n34\n40");
        Comparator<String> comparator = Comparator.naturalOrder();
        Serializer<String> ser = Serializer.linesUtf8();
        File c = new File("target/c");
        Util.findComplement(b, a, ser, comparator, c);
        assertEquals("22\n40\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    private static File write(String filename, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(filename)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return new File(filename);
    }

    @Test
    public void testSplitByCount() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        List<File> files = Util.splitByCount(a, Serializer.linesUtf8(), 2);
        assertEquals(3, files.size());
        assertEquals("1\n2\n", text(files.get(0)));
        assertEquals("3\n4\n", text(files.get(1)));
        assertEquals("5\n", text(files.get(2)));
    }

    @Test
    public void testSplitByCountLarge() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        List<File> files = Util.splitByCount(a, Serializer.linesUtf8(), 6);
        assertEquals(1, files.size());
        assertEquals("1\n2\n3\n4\n5\n", text(files.get(0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitByCountThrowsIfCountZero() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        Util.splitByCount(a, Serializer.linesUtf8(), 0);
    }
    
    @Test
    public void testSplitByCountNoInputs() throws IOException {
        List<File> files = Util.splitByCount(Collections.emptyList(), Serializer.linesUtf8(), n -> new File("target/result" + n), 4);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testSplitBySizeNoInputs() throws IOException {
        List<File> files = Util.splitBySize(Collections.emptyList(), Serializer.linesUtf8(), n -> new File("target/result" + n), 4);
        assertTrue(files.isEmpty());
    }
    
    @Test
    public void testSplitBySize() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        List<File> files = Util.splitBySize(a, Serializer.linesUtf8(), 4);
        assertEquals(3, files.size());
        assertEquals("1\n2\n", text(files.get(0)));
        assertEquals("3\n4\n", text(files.get(1)));
        assertEquals("5\n", text(files.get(2)));
    }

    @Test
    public void testSplitBySizeSlightlyBigger() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        List<File> files = Util.splitBySize(a, Serializer.linesUtf8(), 5);
        assertEquals(3, files.size());
        assertEquals("1\n2\n", text(files.get(0)));
        assertEquals("3\n4\n", text(files.get(1)));
        assertEquals("5\n", text(files.get(2)));
    }

    @Test
    public void testSplitBySizeLarge() throws IOException {
        File a = write("target/a", "1\n2\n3\n4\n5");
        List<File> files = Util.splitByCount(a, Serializer.linesUtf8(), 100);
        assertEquals(1, files.size());
        assertEquals("1\n2\n3\n4\n5\n", text(files.get(0)));
    }

    @Test
    public void testClose() {
        AtomicBoolean closed = new AtomicBoolean();
        Closeable c = new Closeable() {

            @Override
            public void close() throws IOException {
                closed.set(true);
            }
        };
        Util.close(c);
        assertTrue(closed.get());
    }

    @Test(expected = UncheckedIOException.class)
    public void testCloseThrowsUncheckedIOException() {
        Closeable c = new Closeable() {

            @Override
            public void close() throws IOException {
                throw new IOException("boo");
            }
        };
        Util.close(c);
    }

    @Test
    public void testToRuntimeException() {
        RuntimeException e = new RuntimeException();
        assertTrue(e == Util.toRuntimeException(e));
    }

    @Test
    public void testToRuntimeExceptionWithIOException() {
        IOException e = new IOException();
        RuntimeException e2 = Util.toRuntimeException(e);
        assertTrue(e2 instanceof UncheckedIOException);
        assertTrue(e == e2.getCause());
    }
    
    @Test
    public void testToRuntimeExceptionWithNonIOCheckedException() {
        Exception e = new Exception();
        RuntimeException e2 = Util.toRuntimeException(e);
        assertTrue(e == e2.getCause());
    }

    private static String text(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

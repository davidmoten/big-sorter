package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class UtilTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Util.class);
    }

    @Test
    public void testSameLinesTwoSortedFiles() throws IOException {
        File a = new File("target/a");
        write(a, "12\n23\n34");
        File b = new File("target/b");
        write(b, "12\n22\n34\n40");
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
        File a = new File("target/a");
        write(a, "12\n23\n34");
        File b = new File("target/b");
        write(b, "12\n22\n34\n40");
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
        File a = new File("target/a");
        write(a, "12\n23\n34");
        File b = new File("target/b");
        write(b, "12\n22\n34\n40");
        File c = new File("target/c");
        Util.findComplement(a, b, Serializer.linesUtf8(), Comparator.naturalOrder(), c);
        assertEquals("23\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    @Test
    public void testComplementLinesTwoSortedFilesSwapped() throws IOException {
        File a = new File("target/a");
        write(a, "12\n23\n34");
        File b = new File("target/b");
        write(b, "12\n22\n34\n40");
        Comparator<String> comparator = Comparator.naturalOrder();
        Serializer<String> ser = Serializer.linesUtf8();
        File c = new File("target/c");
        Util.findComplement(b, a, ser, comparator, c);
        assertEquals("22\n40\n", (new String(Files.readAllBytes(c.toPath()))));
    }

    private static void write(File file, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}

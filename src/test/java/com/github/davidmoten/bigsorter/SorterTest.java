package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

public class SorterTest {

    private static final File OUTPUT = new File("target/out.txt");

    @Test
    public void test() throws IOException {
        assertEquals("1234", sort("1432"));
    }

    @Test
    public void testEmpty() throws IOException {
        assertEquals("", sort(""));
    }

    @Test
    public void testOne() throws IOException {
        assertEquals("1", sort("1"));
    }

    @Test
    public void testTwo() throws IOException {
        assertEquals("12", sort("21"));
    }

    @Test
    public void testThree() throws IOException {
        assertEquals("123", sort("231"));
    }

    @Test
    public void testFour() throws IOException {
        assertEquals("1234", sort("2431"));
    }

    @Test
    public void testDuplicatesPreserved() throws IOException {
        assertEquals("122234", sort("242312"));
    }

    @Test
    public void testLines() throws IOException {
        assertEquals("ab\nc\ndef", sortLines("c\ndef\nab"));
    }

    @Test
    public void testLinesReverse() throws IOException {
        assertEquals("def\nc\nab", sortLinesReverse("c\ndef\nab"));
    }

    @Test
    public void testLinesReverseWithCharset() throws IOException {
        assertEquals("def\nc\nab", sortLinesReverse("c\ndef\nab", StandardCharsets.UTF_8));
    }

    @Test
    public void testLinesCustomCharset() throws IOException {
        assertEquals("ab\nc\ndef", sortLines("c\ndef\nab", StandardCharsets.UTF_8));
    }

    @Test
    public void testJavaSerializer() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Writer<Serializable> writer = Serializer.java().createWriter(bytes);
        writer.write(Long.valueOf(3));
        writer.write(Long.valueOf(1));
        writer.write(Long.valueOf(2));
        writer.close();

        InputStream in = new ByteArrayInputStream(bytes.toByteArray());
        Sorter //
                .serializer(Serializer.<Long>java()) //
                .comparator(Comparator.naturalOrder()) //
                .input(in) //
                .output(OUTPUT) //
                .sort();

        Reader<Long> reader = Serializer.<Long>java().createReader(new FileInputStream(OUTPUT));
        assertEquals(1, (long) reader.read());
        assertEquals(2, (long) reader.read());
        assertEquals(3, (long) reader.read());
        assertNull(reader.read());
    }

    @Test
    public void testFixedSizeRecordSerializer() throws IOException {
        byte[] b = new byte[] { 8, 2, 3, 4, 2, 7, 3, 9 };
        Serializer<byte[]> serializer = Serializer.fixedSizeRecord(2);
        InputStream in = new ByteArrayInputStream(b);
        Sorter //
                .serializer(serializer) //
                .comparator((x, y) -> x[0] < y[0] ? -1 : (x[0] == y[0] ? 0 : 1)) //
                .input(in) //
                .output(OUTPUT) //
                .sort();
        Reader<byte[]> reader = serializer.createReader(new FileInputStream(OUTPUT));
        assertEquals(2, reader.read()[0]);
        assertEquals(3, reader.read()[0]);
        assertEquals(3, reader.read()[0]);
        assertEquals(8, reader.read()[0]);
        assertNull(reader.read());
    }

    @Test(expected = UncheckedIOException.class)
    public void testInputFileDoesNotExist() {
        File input = new File("target/inputDoesNotExist");
        Sorter.linesUtf8() //
                .input(input) //
                .output(OUTPUT) //
                .sort();
    }

    @Test(expected = UncheckedIOException.class)
    public void testInputStreamThrows() {
        InputStream input = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("boo");
            }
        };
        Sorter.linesUtf8() //
                .input(input) //
                .output(OUTPUT) //
                .sort();
    }

    @Test
    public void testDataSerializer() throws IOException {
        Serializer<Pair> serializer = new DataSerializer<Pair>() {

            @Override
            public Pair read(DataInputStream dis) throws IOException {
                final long a;
                try {
                    a = dis.readLong();
                } catch (EOFException e) {
                    return null;
                }
                long b = dis.readLong();
                return new Pair(a, b);
            }

            @Override
            public void write(DataOutputStream dos, Pair pair) throws IOException {
                dos.writeLong(pair.a);
                dos.writeLong(pair.b);
            }
        };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Writer<Pair> writer = serializer.createWriter(bytes);
        writer.write(new Pair(3, 1));
        writer.write(new Pair(2, 5));
        writer.close();

        InputStream in = new ByteArrayInputStream(bytes.toByteArray());

        Sorter //
                .serializer(serializer) //
                .comparator((x, y) -> Long.compare(x.a, y.a)) //
                .input(in) //
                .output(OUTPUT) //
                .sort();

        Reader<Pair> reader = serializer.createReader(new FileInputStream(OUTPUT));
        assertEquals(2, (long) reader.read().a);
        assertEquals(3, (long) reader.read().a);
        assertNull(reader.read());
    }

    private static final class Pair {
        final long a;
        final long b;

        Pair(long a, long b) {
            this.a = a;
            this.b = b;
        }
    }

    private static String sortLines(String s) throws IOException {
        Sorter //
                .linesUtf8() //
                .input(s) //
                .output(OUTPUT) //
                .maxFilesPerMerge(3) //
                .maxItemsPerFile(2) //
                .sort();

        return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
    }

    private static String sortLinesReverse(String s, Charset charset) throws IOException {
        Sorter //
                .serializerLines(charset) //
                .comparator(Comparator.reverseOrder()) //
                .input(s) //
                .output(OUTPUT) //
                .maxFilesPerMerge(3) //
                .maxItemsPerFile(2) //
                .sort();

        return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
    }

    private static String sortLinesReverse(String s) throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.reverseOrder()) //
                .input(s) //
                .output(OUTPUT) //
                .maxFilesPerMerge(3) //
                .maxItemsPerFile(2) //
                .sort();

        return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
    }

    private static String sortLines(String s, Charset charset) throws IOException {
        Sorter //
                .lines(charset) //
                .input(s) //
                .output(OUTPUT) //
                .maxFilesPerMerge(3) //
                .maxItemsPerFile(2) //
                .bufferSize(128) //
                .sort();

        return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
    }

    private static String sort(String s) throws IOException {
        File f = new File("target/temp.txt");
        writeStringToFile(s, f);
        Serializer<Character> serializer = createCharacterSerializer();
        Sorter //
                .serializer(serializer) //
                .comparator((x, y) -> Character.compare(x, y)) //
                .input(f) //
                .output(OUTPUT) //
                .maxFilesPerMerge(3) //
                .maxItemsPerFile(2) //
                .loggerStdOut() //
                .sort();

        return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
    }

    private static void writeStringToFile(String s, File f) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(f)) {
            out.print(s);
        }
    }

    private static Serializer<Character> createCharacterSerializer() {
        Serializer<Character> serializer = new Serializer<Character>() {

            @Override
            public Reader<Character> createReader(InputStream in) {
                return new Reader<Character>() {

                    java.io.Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);

                    @Override
                    public Character read() throws IOException {
                        int c = r.read();
                        if (c == -1) {
                            return null;
                        } else {
                            return Character.valueOf((char) c);
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        r.close();
                    }

                };
            }

            @Override
            public Writer<Character> createWriter(OutputStream out) {
                return new Writer<Character>() {

                    java.io.Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

                    @Override
                    public void write(Character value) throws IOException {
                        w.write((int) value.charValue());
                    }

                    @Override
                    public void close() throws IOException {
                        w.close();
                    }
                };
            }

        };
        return serializer;
    }

    @Test
    public void testBig() throws IOException {
        final long N = Long.parseLong(System.getProperty("n", "1000000"));
        for (long n = 1000; n <= N; n = n * 10) {
            File input = new File("target/large");

            Serializer<Integer> serializer = new DataSerializer<Integer>() {

                @Override
                public Integer read(DataInputStream dis) throws IOException {
                    try {
                        return dis.readInt();
                    } catch (EOFException e) {
                        return null;
                    }
                }

                @Override
                public void write(DataOutputStream dos, Integer value) throws IOException {
                    dos.writeInt(value);
                }
            };
            long total = 0;
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(input))) {
                Writer<Integer> writer = serializer.createWriter(out);
                SecureRandom r = new SecureRandom();
                for (int i = 0; i < n; i++) {
                    int v = r.nextInt(1000);
                    total += v;
                    writer.write(v);
                }
            }
            long t = System.currentTimeMillis();
            Sorter //
                    .serializer(serializer) //
                    .comparator((x, y) -> Integer.compare(x, y)) //
                    .input(input) //
                    .output(OUTPUT) //
                    .sort();
            System.out.println(
                    n + " integers sorted in " + (System.currentTimeMillis() - t) / 1000.0 + "s");
            // ensure ascending
            try (InputStream in = new BufferedInputStream(new FileInputStream(OUTPUT))) {
                Reader<Integer> reader = serializer.createReader(in);
                Integer v;
                int last = Integer.MIN_VALUE;
                int count = 0;
                long total2 = 0;
                while ((v = reader.read()) != null) {
                    assertTrue(v >= last);
                    total2 += v;
                    last = v;
                    count++;
                }
                assertEquals(n, count);
                assertEquals(total, total2);
            }
            input.delete();
        }
    }

    @Test
    public void testCsv() throws IOException {
        String s = "word1,number,word2\n\"a\",12,\"hello\"\n\"joy\",8,\"there\"";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        Serializer<CSVRecord> ser = Serializer.csv(
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withRecordSeparator("\n"),
                StandardCharsets.UTF_8);
        Comparator<CSVRecord> comparator = (x, y) -> {
            int a = Integer.parseInt(x.get("number"));
            int b = Integer.parseInt(y.get("number"));
            return Integer.compare(a, b);
        };
        Sorter //
                .serializer(ser) //
                .comparator(comparator) //
                .input(in) //
                .output(OUTPUT) //
                .sort();
        printOutput();
        Reader<CSVRecord> r = ser.createReader(new FileInputStream(OUTPUT));
        assertEquals("8", r.read().get(1));
        assertEquals("12", r.read().get(1));
        assertNull(r.read());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxMergeFiles() throws IOException {
        File input = new File("target/input");
        input.createNewFile();
        Sorter.linesUtf8() //
                .input(input) //
                .output(new File("target/output")) //
                .maxFilesPerMerge(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxItemsPerFile() throws IOException {
        File input = new File("target/input");
        input.createNewFile();
        Sorter.linesUtf8() //
                .input(input) //
                .output(new File("target/output")) //
                .maxItemsPerFile(-1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSize() throws IOException {
        File input = new File("target/input");
        input.createNewFile();
        Sorter.linesUtf8() //
                .input(input) //
                .output(new File("target/output")) //
                .bufferSize(0);
    }

    @Test(expected = UncheckedIOException.class)
    public void testJavaSerializerInThrows() throws IOException {
        InputStream in = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("boo");
            }

        };
        Serializer.java().createReader(in).read();
    }

    @Test(expected = UncheckedIOException.class)
    public void testJavaSerializerOutThrows() throws IOException {
        OutputStream out = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw new IOException("boo");
            }
        };
        Serializer.java().createWriter(out).write(Integer.valueOf(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFixedSizeRecordInvalidSize() {
        Serializer.fixedSizeRecord(0);
    }

    static void printOutput() throws IOException {
        String s = new String(Files.readAllBytes(OUTPUT.toPath()));
        System.out.println("output=\n" + s);
    }

}

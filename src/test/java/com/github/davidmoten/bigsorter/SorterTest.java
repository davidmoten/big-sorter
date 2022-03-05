package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.davidmoten.guavamini.Lists;

public class SorterTest {

    private static final File OUTPUT = new File("target/out.txt");

    @Test
    public void test() throws IOException {
        assertEquals("1234", sort("1432"));
    }

    @Test
    public void testWithLogging() throws IOException {
        assertEquals("1234", sortLogging("1432"));
    }

    @Test
    public void testVaryingLineCount() throws IOException {
        for (int n = 0; n < 100; n++) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (s.length() > 0) {
                    s.append("\n");
                }
                s.append(n - i);
            }
            sortLines(s.toString());
        }
        // TODO assert something
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
    public void testLinesFilterAndMap() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder())
                .input("c\ndef\nab") //
                .filter(line -> !line.startsWith("a")) //
                .map(line -> "x" + line) //
                .output(OUTPUT) //
                .sort();
        assertEquals("xc\nxdef", readOutput());
    }
    
    @Test
    public void testLinesFilterThenMapOrderedCorrectly() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder())
                .input("c\ndef\nab") //
                .filter(line -> !line.startsWith("a")) //
                .map(line -> "a" + line) //
                .output(OUTPUT) //
                .sort();
        assertEquals("ac\nadef", readOutput());
    }
    
    @Test
    public void testLinesFlatMap() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()).input("c\ndef\nab") //
                .filter(line -> !line.startsWith("a")) //
                .flatMap(line -> Lists.newArrayList(line, line)) //
                .output(OUTPUT) //
                .sort();
        assertEquals("c\nc\ndef\ndef", readOutput());
    }
    
    @Test
    public void testLinesStreamTransform() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()) //
                .input("c\ndef\nab") //
                .transformStream(s -> s.filter(line -> !line.startsWith("a"))) //
                .transformStream(s -> s.map(line -> "a" + line)) //
                .output(OUTPUT) //
                .sort();
        assertEquals("ac\nadef", readOutput());
    }
    
    @Test
    public void testUnique() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder())
                .input("c\ndef\nab\nc\nab\nc\ndef\ndef") //
                .output(OUTPUT) //
                .unique() //
                .maxItemsPerFile(2) //
                .sort();
        assertEquals("ab\nc\ndef", readOutput());
    }
    
    @Test
    public void testSupplier() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder())
                .input(() -> new ByteArrayInputStream("c\ndef\nab".getBytes(StandardCharsets.UTF_8))) //
                .output(OUTPUT) //
                .sort();
        assertEquals("ab\nc\ndef", readOutput());
    }
    
    @Test
    public void testMultipleInputs() throws IOException {
        Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder())
                .input("c\ndef", "ab") //
                .output(OUTPUT) //
                .sort();
        assertEquals("ab\nc\ndef", readOutput());
    }
    
    @Test
    public void testReturnAsStream() {
        try (Stream<String> stream = Sorter //
                .serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()).input("c\ndef", "ab") //
                .outputAsStream() //
                .sort()) {
                String s = stream.collect(Collectors.joining("\n"));
                assertEquals("ab\nc\ndef", s);
        }
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

        return readOutput();
    }

    private static String readOutput() throws IOException {
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

        return readOutput();
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

        return readOutput();
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

        return readOutput();
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
                .tempDirectory(new File("target")) //
                .sort();

        return readOutput();
    }

    private static String sortLogging(String s) throws IOException {
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
                .tempDirectory(new File("target")) //
                .loggerStdOut() //
                .sort();

        return readOutput();
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

                    @Override
                    public void flush() throws IOException {
                        w.flush();
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
                    .maxItemsPerFile(10000000) //
                    .initialSortInParallel() //
                    .logger(System.out::println) //
                    .sort();
            System.out.println(n + " integers sorted in " + (System.currentTimeMillis() - t) / 1000.0 + "s");
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
    public void testCsvWithHeader() throws IOException {
        String s = "word1,number,word2\n\"a\",12,\"hello\"\n\"joy\",8,\"there\"";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        Serializer<CSVRecord> ser = Serializer
                .csv(CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build(), StandardCharsets.UTF_8);
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

    @Test
    public void testCsvWithoutHeader() throws IOException {
        String s = "\"a\",12,\"hello\"\n\"joy\",8,\"there\"";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        Serializer<CSVRecord> ser = Serializer.csv(CSVFormat.DEFAULT, StandardCharsets.UTF_8);
        Comparator<CSVRecord> comparator = (x, y) -> {
            int a = Integer.parseInt(x.get(1));
            int b = Integer.parseInt(y.get(1));
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

    @Test(expected = NullPointerException.class)
    public void testInvalidTempDirectory() throws IOException {
        File input = new File("target/input");
        input.createNewFile();
        Sorter.linesUtf8() //
                .input(input) //
                .output(new File("target/output")) //
                .tempDirectory(null);
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

    @Test
    public void testJson() throws IOException {
        String s = "[ {\"name\":\"fred\",\"age\": 23 }, {\"name\":\"anne\",\"age\": 31 } ]";

        Serializer<ObjectNode> ser = Serializer.jsonArray();
        Sorter.serializer(ser) //
                .comparator((x, y) -> x.get("name").asText().compareTo(y.get("name").asText())) //
                .input(s) //
                .output(OUTPUT) //
                .sort();
        Reader<ObjectNode> r = ser.createReader(new FileInputStream(OUTPUT));
        ObjectNode node = r.read();
        assertEquals("anne", node.get("name").asText());
        assertEquals(31, node.get("age").asInt());
        node = r.read();
        assertEquals("fred", node.get("name").asText());
        assertEquals(23, node.get("age").asInt());
        assertNull(r.read());
    }

    @Test(expected = IllegalStateException.class)
    public void testJsonNotArray() throws IOException {
        String s = "{\"name\":\"john\"}";
        Serializer.jsonArray().createReader(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Test(expected = UncheckedIOException.class)
    public void testJsonArrayIoException() throws IOException {
        InputStream in = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("boo");
            }
        };
        Serializer.jsonArray().createReader(in);
    }

    @Test
    public void testJsonArrayWriterCloseTwice() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer<ObjectNode> w = Serializer.jsonArray().createWriter(out)) {
            w.close();
            // should write newline and ]
            assertEquals(2, out.size());
            w.close();
            assertEquals(2, out.size());
        }
    }

    @Test
    public void testJsonArrayWriterFlush() throws IOException {
        boolean[] flushed = new boolean[1];
        OutputStream out = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
            }

            @Override
            public void flush() throws IOException {
                flushed[0] = true;
            }

        };
        Writer<ObjectNode> w = Serializer.jsonArray().createWriter(out);
        assertFalse(flushed[0]);
        w.flush();
        assertTrue(flushed[0]);
    }

    @Test(expected = UncheckedIOException.class)
    public void testMergeFileWhenDoesNotExist() {
        List<Supplier<? extends Reader<? extends String>>> list = Collections.singletonList(() ->  
                emptyReader());
        Sorter<String> sorter = new Sorter<String>(list, Serializer.linesUtf8(),
                OUTPUT, Comparator.naturalOrder(), 3, 1000, x -> {
                }, 8192, new File(System.getProperty("java.io.tmpdir")), false, false, Optional.empty());
        sorter.merge(Lists.newArrayList(new File("target/doesnotexist"), new File("target/doesnotexist2")));
    }
    
    @Test
    public void testSortOfItems() {
        List<String> list = Sorter.serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()) //
                .inputItems("bbb", "aaa", "ddd", "ccc") //
                .outputAsStream() //
                .sort() //
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd"), list);
    }
    
    @Test
    public void testSortOfList() {
        List<String> list = Sorter.serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()) //
                .inputItems(Arrays.asList("bbb", "aaa", "ddd", "ccc")) //
                .outputAsStream() //
                .sort() //
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd"), list);
    }
    
    @Test
    public void testSortOfIterator() {
        List<String> list = Sorter.serializerLinesUtf8() //
                .comparator(Comparator.naturalOrder()) //
                .inputItems(Arrays.asList("bbb", "aaa", "ddd", "ccc").iterator()) //
                .outputAsStream() //
                .sort() //
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd"), list);
    }
    
    private static Reader<String> emptyReader() {
        return new Reader<String>() {

            @Override
            public void close() throws IOException {
               // do nothing
            }

            @Override
            public String read() throws IOException {
                return null;
            }
            
        };
    }

    @Test(expected=RuntimeException.class)
    public void testSortAsStreamThrows() {
        Sorter.linesUtf8() //
                .input(() -> {
                    throw new RuntimeException();
                }) //
                .outputAsStream() //
                .sort();
    }
    
    @Test
    public void testSortIntegersFromFile() {
        Sorter //
          .serializerLinesUtf8() //
          .comparator((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b))) //
          .input(new File("src/test/resources/numbers.txt")) //
          .filter(line -> !line.isEmpty()) //
          .output(new File("target/numbers-sorted.txt")) //
          .sort();
    }
    
    @Test
    public void testSortIntegersFromFileMoreEfficient() throws IOException {
        File textInts = new File("src/test/resources/numbers.txt");
        
        Serializer<Integer> intSerializer = Serializer.dataSerializer( //
                dis -> (Integer) dis.readInt(), //
                (dos, v) -> dos.writeInt(v));
        
        List<Integer> list = Sorter //
                .serializer(intSerializer) //
                .inputMapper(Serializer.linesUtf8(), line -> Integer.parseInt(line)) //
                .naturalOrder() //
                .input(textInts) //
                .outputAsStream() //
                .sort() //
                .collect(Collectors.toList());
        
        assertEquals(10, list.size());
        assertEquals(66904383, (int) list.get(0));
        assertEquals(1956321588, (int) list.get(list.size() -1 ));
    }
    
    @Test
    public void testSortIntegersFromFileMoreEfficientOutputIsFileMappedBackToStringLines() throws IOException {
        Serializer<Integer> intSerializer = Serializer.dataSerializer( //
                dis -> (Integer) dis.readInt(), //
                (dos, v) -> dos.writeInt(v));
        
        File output = new File("target/output");
        Sorter //
                .serializer(intSerializer) //
                .inputMapper(Serializer.linesUtf8(), line -> Integer.parseInt(line)) //
                .naturalOrder() //
                .input("456","123", "234") //
                .output(output) //
                .outputMapper(Serializer.linesUtf8(), x -> Integer.toString(x)) //
                .sort();
        List<String> list = Files.readAllLines(output.toPath());
        assertEquals(Arrays.asList("123", "234", "456"), list);
    }
    
    static void printOutput() throws IOException {
        String s = new String(Files.readAllBytes(OUTPUT.toPath()));
        System.out.println("output=\n" + s);
    }

}

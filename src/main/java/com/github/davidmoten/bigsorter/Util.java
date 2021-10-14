package com.github.davidmoten.bigsorter;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.davidmoten.bigsorter.Sorter.AbstractFile;
import com.github.davidmoten.bigsorter.Sorter.StandardFile;
import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Preconditions;

public final class Util {

    private Util() {
        // prevent instantiation
    }

    /**
     * Writes common entries from both readers to the writer in sorted
     * order.{@code readerA} and {@code readerB} must be reading already sorted
     * data.
     * 
     * @param <T>        item type
     * @param readerA    reader of first file
     * @param readerB    reader of second file
     * @param comparator comparator for item
     * @param writer     writer to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findSame(Reader<? extends T> readerA, Reader<? extends T> readerB,
            Comparator<? super T> comparator, Writer<T> writer) throws IOException {
        // return A intersection B
        T x = readerA.read();
        T y = readerB.read();
        while (x != null && y != null) {
            int compare = comparator.compare(x, y);
            if (compare == 0) {
                writer.write(x);
                // read next values
                x = readerA.read();
                y = readerB.read();
            } else if (compare < 0) {
                x = readerA.read();
            } else {
                y = readerB.read();
            }
        }
    }

    /**
     * Writes common entries from both files to the output file in sorted
     * order.{@code a} and {@code b} must already be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findSame(AbstractFile a, AbstractFile b, Serializer<T> serializer,
            Comparator<? super T> comparator, AbstractFile output) throws IOException {
        try (Reader<T> readerA = serializer.createReader(a.createInputStream());
                Reader<T> readerB = serializer.createReader(b.createInputStream());
                Writer<T> writer = serializer.createWriter(output.createOutputStream())) {
            Util.findSame(readerA, readerB, comparator, writer);
        }
    }

    /**
     * Writes common entries from both files to the output file in sorted
     * order.{@code a} and {@code b} must already be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findSame(File a, File b, Serializer<T> serializer,
            Comparator<? super T> comparator, File output) throws IOException {
        findSame(new StandardFile(a), new StandardFile(b), serializer, comparator,
                new StandardFile(output));
    }

    /**
     * Writes different entries (only those entries that are only present in one
     * input reader) from both readers to the writer in sorted order.
     * {@code readerA} and {@code readerB} must be reading already sorted data.
     * 
     * @param <T>        item type
     * @param readerA    reader of first file
     * @param readerB    reader of second file
     * @param comparator comparator for item
     * @param writer     writer to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findDifferent(Reader<? extends T> readerA, Reader<? extends T> readerB,
            Comparator<? super T> comparator, Writer<T> writer) throws IOException {
        // returns those elements in (A union B) \ (A intersection B)
        T x = readerA.read();
        T y = readerB.read();
        while (x != null && y != null) {
            int compare = comparator.compare(x, y);
            if (compare == 0) {
                x = readerA.read();
                y = readerB.read();
            } else if (compare < 0) {
                writer.write(x);
                x = readerA.read();
            } else {
                writer.write(y);
                y = readerB.read();
            }
        }
        while (x != null) {
            writer.write(x);
            x = readerA.read();
        }
        while (y != null) {
            writer.write(y);
            y = readerB.read();
        }
    }

    /**
     * Writes different entries (only those entries that are only present in one
     * file) from both files to the output file in sorted order. {@code a} and
     * {@code b} must already be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findDifferent(AbstractFile a, AbstractFile b, Serializer<T> serializer,
            Comparator<? super T> comparator, AbstractFile output) throws IOException {
        try (Reader<T> readerA = serializer.createReader(a.createInputStream());
                Reader<T> readerB = serializer.createReader(b.createInputStream());
                Writer<T> writer = serializer.createWriter(output.createOutputStream())) {
            Util.findDifferent(readerA, readerB, comparator, writer);
        }
    }
    
    /**
     * Writes different entries (only those entries that are only present in one
     * file) from both files to the output file in sorted order. {@code a} and
     * {@code b} must already be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findDifferent(File a, File b, Serializer<T> serializer,
            Comparator<? super T> comparator, File output) throws IOException {
        findDifferent(new StandardFile(a), new StandardFile(b), serializer, comparator,
                new StandardFile(output));
    }

    /**
     * Writes to the output file only those entries from the first reader that are
     * not present in the second reader. {@code readerA} and {@code readerB} must be
     * reading already sorted data.
     * 
     * @param <T>        item type
     * @param readerA    reader of first file
     * @param readerB    reader of second file
     * @param comparator comparator for item
     * @param writer     writer to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findComplement(Reader<? extends T> readerA, Reader<? extends T> readerB,
            Comparator<? super T> comparator, Writer<T> writer) throws IOException {
        // returns those elements in A that are not present in B
        T x = readerA.read();
        T y = readerB.read();
        while (x != null && y != null) {
            int compare = comparator.compare(x, y);
            if (compare == 0) {
                x = readerA.read();
                y = readerB.read();
            } else if (compare < 0) {
                writer.write(x);
                x = readerA.read();
            } else {
                y = readerB.read();
            }
        }
        while (x != null) {
            writer.write(x);
            x = readerA.read();
        }
    }

    /**
     * Writes only those entries that are in the first file but not in the second
     * file to the output file in sorted order. {@code a} and {@code b} must already
     * be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findComplement(AbstractFile a, AbstractFile b, Serializer<T> serializer,
            Comparator<? super T> comparator, AbstractFile output) throws IOException {
        try (Reader<T> readerA = serializer.createReader(a.createInputStream());
                Reader<T> readerB = serializer.createReader(b.createInputStream());
                Writer<T> writer = serializer.createWriter(output.createOutputStream())) {
            Util.findComplement(readerA, readerB, comparator, writer);
        }
    }
    
    /**
     * Writes only those entries that are in the first file but not in the second
     * file to the output file in sorted order. {@code a} and {@code b} must already
     * be sorted.
     * 
     * @param <T>        item type
     * @param a          first file
     * @param b          second file
     * @param serializer item serializer
     * @param comparator comparator for item
     * @param output     file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findComplement(File a, File b, Serializer<T> serializer,
            Comparator<? super T> comparator, File output) throws IOException {
        findComplement(new StandardFile(a), new StandardFile(b), serializer, comparator,
                new StandardFile(output));
    }

    public static <T> List<AbstractFile> splitByCount(AbstractFile input, Serializer<T> serializer,
            long count) throws IOException {
        return splitByCount( //
                input, //
                serializer, //
                n -> input.sibling(input.name() + "-" + n), //
                count);
    }
    
    public static <T> List<File> splitByCount(File input, Serializer<T> serializer,
            long count) throws IOException {
        return splitByCount(new StandardFile(input), serializer, count).stream()
                .map(x -> ((StandardFile) x).file()).collect(Collectors.toList());
    }

    public static <T> List<AbstractFile> splitByCount(AbstractFile input, Serializer<T> serializer,
            Function<Integer, AbstractFile> output, long count) throws IOException {
        return splitByCount_(Collections.singletonList(input), serializer, output, count);
    }

    public static <T> List<AbstractFile> splitByCount_(List<AbstractFile> input,
            Serializer<T> serializer, Function<Integer, AbstractFile> output, long count)
            throws IOException {
        Preconditions.checkArgument(count > 0, "count must be greater than 0");
        List<AbstractFile> list = Lists.newArrayList();
        T t;
        int fileNumber = 0;
        long i = 0;
        Writer<T> writer = null;
        try {
            for (AbstractFile file : input) {
                try (Reader<T> reader = serializer.createReader(file)) {
                    while ((t = reader.read()) != null) {
                        if (writer == null) {
                            fileNumber++;
                            AbstractFile f = output.apply(fileNumber);
                            list.add(f);
                            writer = serializer.createWriter(f);
                        }
                        writer.write(t);
                        i++;
                        if (i == count) {
                            writer.close();
                            writer = null;
                            i = 0;
                        }
                    }
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return list;
    }
    
    public static <T> List<File> splitByCount(List<File> input,
            Serializer<T> serializer, Function<Integer, File> output, long count)
            throws IOException {
        return splitByCount_(input.stream().map(x -> new StandardFile(x)).collect(Collectors.toList()),
                serializer, x -> new StandardFile(output.apply(x)), count) //
                .stream().map(x -> ((StandardFile) x).file()).collect(Collectors.toList());
    }

    public static <T> List<AbstractFile> splitBySize(AbstractFile input, Serializer<T> serializer,
            long maxSize) throws IOException {
        return splitBySize( //
                input, //
                serializer, //
                n -> input.sibling(input.name() + "-" + n), //
                maxSize);
    }
    
    public static <T> List<File> splitBySize(File input, Serializer<T> serializer,
            long maxSize) throws IOException {
        return splitBySize(new StandardFile(input), serializer, maxSize).stream()
                .map(x -> ((StandardFile) x).file()).collect(Collectors.toList());
    }

    public static <T> List<AbstractFile> splitBySize(AbstractFile input, Serializer<T> serializer,
            Function<Integer, AbstractFile> output, long maxSize) throws IOException {
        return splitBySize_(Collections.singletonList(input), serializer, output, maxSize);
    }

    public static <T> List<AbstractFile> splitBySize_(List<AbstractFile> input,
            Serializer<T> serializer, Function<Integer, AbstractFile> output, long maxSize)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Writer<T> buffer = serializer.createWriter(bytes);
        List<AbstractFile> list = Lists.newArrayList();
        T t;
        int fileNumber = 0;
        long n = 0;
        Writer<T> writer = null;
        try {
            for (AbstractFile file : input) {
                try (Reader<T> reader = serializer.createReader(file)) {
                    while ((t = reader.read()) != null) {
                        // check increase in size from writing t
                        // by writing to buffer
                        bytes.reset();
                        buffer.write(t);
                        buffer.flush();
                        n += bytes.size();
                        if (writer == null) {
                            fileNumber++;
                            AbstractFile f = output.apply(fileNumber);
                            list.add(f);
                            writer = serializer.createWriter(f);
                            n = bytes.size();
                        } else if (n > maxSize) {
                            writer.close();
                            fileNumber++;
                            AbstractFile f = output.apply(fileNumber);
                            list.add(f);
                            writer = serializer.createWriter(f);
                            n = bytes.size();
                        }
                        writer.write(t);
                    }
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return list;
    }
    
    public static <T> List<File> splitBySize(List<File> input,
            Serializer<T> serializer, Function<Integer, File> output, long maxSize)
            throws IOException {
        return splitBySize_(input.stream().map(x -> new StandardFile(x)).collect(Collectors.toList()),
                serializer, x -> new StandardFile(output.apply(x)), maxSize) //
                .stream().map(x -> ((StandardFile) x).file()).collect(Collectors.toList());
    }

    static void close(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static RuntimeException toRuntimeException(Throwable e) {
        if (e instanceof IOException) {
            return new UncheckedIOException((IOException) e);
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e);
        }
    }
}

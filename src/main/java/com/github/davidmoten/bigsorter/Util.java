package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

public final class Util {

    private Util() {
        // prevent instantiation
    }

    /**
     * Writes common entries from both readers to the writer in sorted
     * order.{@code readerA} and {@code readerB} must be reading already sorted
     * data.
     * 
     * @param <T>
     *            item type
     * @param readerA
     *            reader of first file
     * @param readerB
     *            reader of second file
     * @param comparator
     *            comparator for item
     * @param writer
     *            writer to which common entries are written to
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
     * @param <T>
     *            item type
     * @param a
     *            first file
     * @param b
     *            second file
     * @param serializer
     *            item serializer
     * @param comparator
     *            comparator for item
     * @param output
     *            file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findSame(File a, File b, Serializer<T> serializer, Comparator<? super T> comparator, File output) throws IOException {
        try (
          Reader<T> readerA = serializer.createReader(a); 
          Reader<T> readerB = serializer.createReader(b);
          Writer<T> writer = serializer.createWriter(output)) {
            Util.findSame(readerA, readerB, comparator, writer);
        }
    }

    /**
     * Writes different entries (only those entries that are only present in one
     * input reader) from both readers to the writer in sorted order.
     * {@code readerA} and {@code readerB} must be reading already sorted data.
     * 
     * @param <T>
     *            item type
     * @param readerA
     *            reader of first file
     * @param readerB
     *            reader of second file
     * @param comparator
     *            comparator for item
     * @param writer
     *            writer to which common entries are written to
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
     * file) from both files to the output file in sorted order.
     * {@code a} and {@code b} must already be sorted.
     * 
     * @param <T>
     *            item type
     * @param a
     *            first file
     * @param b
     *            second file
     * @param serializer
     *            item serializer
     * @param comparator
     *            comparator for item
     * @param output
     *            file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findDifferent(File a, File b, Serializer<T> serializer, Comparator<? super T> comparator, File output) throws IOException {
        try (
          Reader<T> readerA = serializer.createReader(a); 
          Reader<T> readerB = serializer.createReader(b);
          Writer<T> writer = serializer.createWriter(output)) {
            Util.findDifferent(readerA, readerB, comparator, writer);
        }
    }

    
    /**
     * Writes to the output file only those entries from the first reader that are not present in the 
     * second reader. {@code readerA} and {@code readerB} must be reading already sorted data.
     * 
     * @param <T>
     *            item type
     * @param readerA
     *            reader of first file
     * @param readerB
     *            reader of second file
     * @param comparator
     *            comparator for item
     * @param writer
     *            writer to which common entries are written to
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
     * Writes only those entries that are in the first file but not in the second file 
     * to the output file in sorted order. {@code a} and {@code b} must already be sorted.
     * 
     * @param <T>
     *            item type
     * @param a
     *            first file
     * @param b
     *            second file
     * @param serializer
     *            item serializer
     * @param comparator
     *            comparator for item
     * @param output
     *            file to which common entries are written to
     * @throws IOException I/O exception
     */
    public static <T> void findComplement(File a, File b, Serializer<T> serializer, Comparator<? super T> comparator, File output) throws IOException {
        try (
          Reader<T> readerA = serializer.createReader(a); 
          Reader<T> readerB = serializer.createReader(b);
          Writer<T> writer = serializer.createWriter(output)) {
            Util.findComplement(readerA, readerB, comparator, writer);
        }
    }
}

package com.github.davidmoten.bigsorter;

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
     * @throws IOException
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
     * Writes different entries (only those entries that are only present in one
     * input reader) from both readers to the writer in sorted order.
     * {@code readerA} and {@code readerB} must be reading already sorted data.
     * 
     * @param <T>
     *            item type
     * @param readerA
     *            reader of first file
     * @param readerA
     *            reader of second file
     * @param comparator
     *            comparator for item
     * @param writer
     *            writer to which common entries are written to
     * @throws IOException
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
     * input reader) from both readers to the writer in sorted order.
     * {@code readerA} and {@code readerB} must be reading already sorted data.
     * 
     * @param <T>
     *            item type
     * @param readerA
     *            reader of first file
     * @param readerA
     *            reader of second file
     * @param comparator
     *            comparator for item
     * @param writer
     *            writer to which common entries are written to
     * @throws IOException
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

}

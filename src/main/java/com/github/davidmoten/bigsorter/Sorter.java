package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import com.github.davidmoten.guavamini.Preconditions;

public final class Sorter<T> {

    private final File file;
    private final Serializer<T> serializer;
    private final File output;
    private final Comparator<T> comparator;
    private final int maxFilesPerMerge;
    private final int maxItemsPerPart;

    public Sorter(File file, Serializer<T> serializer, File output, Comparator<T> comparator,
            int maxFilesPerMerge, int maxItemsPerPart) {
        this.file = file;
        this.serializer = serializer;
        this.output = output;
        this.comparator = comparator;
        this.maxFilesPerMerge = maxFilesPerMerge;
        this.maxItemsPerPart = maxItemsPerPart;
    }

    public void sort() {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            sort(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void sort(InputStream in) throws IOException {
        // read the input into sorted small files
        List<File> files = new ArrayList<>();
        {
            int i = 0;
            List<T> list = new ArrayList<>();
            while (true) {
                T t = serializer.read(in);
                if (t != null) {
                    list.add(t);
                    i++;
                }
                if (t == null || i == maxItemsPerPart) {
                    i = 0;
                    if (list.size() > 0) {
                        File f = sortAndWriteToFile(list);
                        files.add(f);
                        list.clear();
                    }
                }
                if (t == null) {
                    break;
                }
            }
        }

        // merge the files in chunks repeatededly until only one remains
        while (files.size() > 1) {
            List<File> nextRound = new ArrayList<>();
            for (int i = 0; i < files.size(); i += maxFilesPerMerge) {
                File merged = merge(
                        files.subList(i, Math.min(files.size() - 1, i + maxFilesPerMerge)));
                nextRound.add(merged);
            }
            files = nextRound;
        }
        Files.move( //
                files.get(0).toPath(), //
                output.toPath(), //
                StandardCopyOption.ATOMIC_MOVE, //
                StandardCopyOption.REPLACE_EXISTING);
    }

    private File merge(List<File> list) throws IOException {
        Preconditions.checkArgument(!list.isEmpty());
        if (list.size() == 1) {
            return list.get(0);
        }
        List<State<T>> states = list //
                .stream() //
                .map(f -> {
                    try {
                        return createState(f);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                }) //
                .filter(x -> x.value != null) //
                .collect(Collectors.toList());
        File output = nextTempFile();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
            PriorityQueue<State<T>> q = new PriorityQueue<>(states.size(), (x, y) -> compare(x, y));
            while (!q.isEmpty()) {
                State<T> state = q.poll();
                serializer.write(out, state.value);
                state.value = serializer.read(state.in);
                if (state.value != null) {
                    q.offer(state);
                }
            }
        }
        return output;
    }

    private int compare(State<T> x, State<T> y) {
        if (x.value == null && y.value == null) {
            return 0;
        } else if (x.value == null) {
            return -1;
        } else if (y.value == null) {
            return 1;
        } else {
            return comparator.compare(x.value, y.value);
        }
    }

    private State<T> createState(File f) throws FileNotFoundException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        T t = serializer.read(in);
        return new State<T>(in, t);
    }

    private static final class State<T> {
        InputStream in;
        T value;

        State(InputStream in, T value) {
            this.in = in;
            this.value = value;
        }
    }

    private File sortAndWriteToFile(List<T> list) throws FileNotFoundException, IOException {
        Collections.sort(list, comparator);
        File file = nextTempFile();
        writeToFile(list, file);
        return file;
    }

    private void writeToFile(List<T> list, File f) throws FileNotFoundException, IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            for (T t : list) {
                serializer.write(out, t);
            }
        }
    }

    private File nextTempFile() throws IOException {
        return File.createTempFile("big-sorter", ".bin");
    }

}

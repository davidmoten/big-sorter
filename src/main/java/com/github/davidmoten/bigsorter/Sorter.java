package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

//NotThreadSafe
public final class Sorter<T> {

    private final InputStream input;
    private final Serializer<T> serializer;
    private final File output;
    private final Comparator<? super T> comparator;
    private final int maxFilesPerMerge;
    private final int maxItemsPerPart;
    private final Consumer<? super String> log;
    private final int bufferSize;
    private final File tempDirectory;
    private long count = 0;

    Sorter(InputStream input, Serializer<T> serializer, File output,
            Comparator<? super T> comparator, int maxFilesPerMerge, int maxItemsPerFile,
            Consumer<? super String> log, int bufferSize, File tempDirectory) {
        Preconditions.checkNotNull(input, "input must be specified");
        Preconditions.checkNotNull(serializer, "serializer must be specified");
        Preconditions.checkNotNull(output, "output must be specified");
        Preconditions.checkNotNull(comparator, "comparator must be specified");
        this.input = input;
        this.serializer = serializer;
        this.output = output;
        this.comparator = comparator;
        this.maxFilesPerMerge = maxFilesPerMerge;
        this.maxItemsPerPart = maxItemsPerFile;
        this.log = log;
        this.bufferSize = bufferSize;
        this.tempDirectory = tempDirectory;
    }

    public static <T> Builder<T> serializer(Serializer<T> serializer) {
        Preconditions.checkNotNull(serializer, "serializer cannot be null");
        return new Builder<T>(serializer);
    }

    public static <T> Builder<String> serializerLinesUtf8() {
        return serializer(Serializer.linesUtf8());
    }

    public static <T> Builder<String> serializerLines(Charset charset) {
        return serializer(Serializer.lines(charset));
    }

    public static <T> Builder2<String> lines(Charset charset) {
        return serializer(Serializer.lines(charset)).comparator(Comparator.naturalOrder());
    }

    public static <T> Builder2<String> linesUtf8() {
        return serializer(Serializer.linesUtf8()).comparator(Comparator.naturalOrder());
    }

    public static final class Builder<T> {
        private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss.Sxxxx");
        private InputStream input;
        private final Serializer<T> serializer;
        private File output;
        private Comparator<? super T> comparator;
        private int maxFilesPerMerge = 100;
        private int maxItemsPerFile = 100000;
        private File inputFile;
        private Consumer<? super String> logger = null;
        private int bufferSize = 8192;
        private File tempDirectory = new File(System.getProperty("java.io.tmpdir"));

        Builder(Serializer<T> serializer) {
            this.serializer = serializer;
        }

        public Builder2<T> comparator(Comparator<? super T> comparator) {
            Preconditions.checkNotNull(comparator, "comparator cannot be null");
            this.comparator = comparator;
            return new Builder2<T>(this);
        }
    }

    public static final class Builder2<T> {
        private final Builder<T> b;

        Builder2(Builder<T> b) {
            this.b = b;
        }

        public Builder3<T> input(String string, Charset charset) {
            Preconditions.checkNotNull(string, "string cannot be null");
            Preconditions.checkNotNull(charset, "charset cannot be null");
            return input(new ByteArrayInputStream(string.getBytes(charset)));
        }

        public Builder3<T> input(String string) {
            Preconditions.checkNotNull(string);
            return input(string, StandardCharsets.UTF_8);
        }

        public Builder3<T> input(InputStream input) {
            Preconditions.checkNotNull(input, "input cannot be null");
            b.input = input;
            return new Builder3<T>(b);
        }

        public Builder3<T> input(File inputFile) {
            Preconditions.checkNotNull(inputFile, "inputFile cannot be null");
            b.inputFile = inputFile;
            return new Builder3<T>(b);
        }

    }

    public static final class Builder3<T> {
        private final Builder<T> b;

        Builder3(Builder<T> b) {
            this.b = b;
        }

        public Builder4<T> output(File output) {
            Preconditions.checkNotNull(output, "output cannot be null");
            b.output = output;
            return new Builder4<T>(b);
        }

    }

    public static final class Builder4<T> {
        private final Builder<T> b;

        Builder4(Builder<T> b) {
            this.b = b;
        }

        public Builder4<T> maxFilesPerMerge(int value) {
            Preconditions.checkArgument(value > 1, "maxFilesPerMerge must be greater than 1");
            b.maxFilesPerMerge = value;
            return this;
        }

        public Builder4<T> maxItemsPerFile(int value) {
            Preconditions.checkArgument(value > 0, "maxItemsPerFile must be greater than 0");
            b.maxItemsPerFile = value;
            return this;
        }

        public Builder4<T> logger(Consumer<? super String> logger) {
            Preconditions.checkNotNull(logger, "logger cannot be null");
            b.logger = logger;
            return this;
        }

        // public Builder4<T> async() {
        // b.executor = Executors.newSingleThreadExecutor();
        // return this;
        // }

        public Builder4<T> loggerStdOut() {
            return logger(new Consumer<String>() {

                @Override
                public void accept(String msg) {
                    System.out.println(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                            .format(Builder.DATE_TIME_PATTERN) + " " + msg);
                }
            });
        }

        public Builder4<T> bufferSize(int bufferSize) {
            Preconditions.checkArgument(bufferSize > 0, "bufferSize must be greater than 0");
            b.bufferSize = bufferSize;
            return this;
        }

        public Builder4<T> tempDirectory(File directory) {
            Preconditions.checkNotNull(directory, "tempDirectory cannot be null");
            b.tempDirectory = directory;
            return this;
        }

        /**
         * Sorts the input and writes the result to the given output file. If an
         * {@link IOException} occurs then it is thrown wrapped in
         * {@link UncheckedIOException}.
         */
        public void sort() {
            try {
                if (b.inputFile != null) {
                    try (InputStream in = openFile(b.inputFile, b.bufferSize)) {
                        sort(in);
                    }
                } else {
                    sort(b.input);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void sort(InputStream input) {
            Sorter<T> sorter = new Sorter<T>(input, b.serializer, b.output, b.comparator,
                    b.maxFilesPerMerge, b.maxItemsPerFile, b.logger, b.bufferSize, b.tempDirectory);
            try {
                sorter.sort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    static InputStream openFile(File file, int bufferSize) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file), bufferSize);
    }

    private void log(String msg, Object... objects) {
        if (log != null) {
            String s = String.format(msg, objects);
            log.accept(s);
        }
    }

    private File sort() throws IOException {

        tempDirectory.mkdirs();

        // read the input into sorted small files
        long time = System.currentTimeMillis();
        count = 0;
        List<File> files = new ArrayList<>();
        log("starting sort");
        try (Reader<T> reader = serializer.createReader(input)) {
            int i = 0;
            List<T> list = new ArrayList<>();
            while (true) {
                T t = reader.read();
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
        log("completed inital split and sort, starting merge");
        File result = merge(files);
        Files.move( //
                result.toPath(), //
                output.toPath(), //
                StandardCopyOption.ATOMIC_MOVE, //
                StandardCopyOption.REPLACE_EXISTING);
        log("sort of " + count + " records completed in "
                + (System.currentTimeMillis() - time) / 1000.0 + "s");
        return output;
    }

    @VisibleForTesting
    File merge(List<File> files) {
        // merge the files in chunks repeatededly until only one remains
        // TODO make a better guess at the chunk size so groups are more even
        try {
            while (files.size() > 1) {
                List<File> nextRound = new ArrayList<>();
                for (int i = 0; i < files.size(); i += maxFilesPerMerge) {
                    File merged = mergeGroup(files.subList(i, Math.min(files.size(), i + maxFilesPerMerge)));
                    nextRound.add(merged);
                }
                files = nextRound;
            }
            File result;
            if (files.isEmpty()) {
                output.delete();
                output.createNewFile();
                result = output;
            } else {
                result = files.get(0);
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private File mergeGroup(List<File> list) throws IOException {
        log("merging %s files", list.size());
        if (list.size() == 1) {
            return list.get(0);
        }
        List<State<T>> states = new ArrayList<>();
        for (File f : list) {
            State<T> st = createState(f);
            // note that st.value will be present otherwise the file would be empty
            // and an empty file would not be passed to this method
            states.add(st);
        }
        File output = nextTempFile();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(output), bufferSize);
                Writer<T> writer = serializer.createWriter(out)) {
            PriorityQueue<State<T>> q = new PriorityQueue<>(
                    (x, y) -> comparator.compare(x.value, y.value));
            q.addAll(states);
            while (!q.isEmpty()) {
                State<T> state = q.poll();
                writer.write(state.value);
                state.value = state.reader.readAutoClosing();
                if (state.value != null) {
                    q.offer(state);
                } else {
                    // delete intermediate files
                    state.file.delete();
                }
            }
            // TODO if an IOException occurs then we should attempt to close and delete
            // temporary files
        }
        return output;
    }

    private State<T> createState(File f) throws IOException {
        InputStream in = openFile(f, bufferSize);
        Reader<T> reader = serializer.createReader(in);
        T t = reader.readAutoClosing();
        return new State<T>(f, reader, t);
    }

    private static final class State<T> {
        final File file;
        Reader<T> reader;
        T value;

        State(File file, Reader<T> reader, T value) {
            this.file = file;
            this.reader = reader;
            this.value = value;
        }
    }

    private File sortAndWriteToFile(List<T> list) throws FileNotFoundException, IOException {
        File file = nextTempFile();
        long t = System.currentTimeMillis();
        Collections.sort(list, comparator);
        writeToFile(list, file);
        DecimalFormat df = new DecimalFormat("0.000");
        count += list.size();
        log("total=%s, sorted %s records to file %s in %ss", //
                count, //
                list.size(), //
                file.getName(), //
                df.format((System.currentTimeMillis() - t) / 1000.0));
        return file;
    }

    private void writeToFile(List<T> list, File f) throws FileNotFoundException, IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f), bufferSize);
                Writer<T> writer = serializer.createWriter(out)) {
            for (T t : list) {
                writer.write(t);
            }
        }
    }

    private File nextTempFile() throws IOException {
        return File.createTempFile("big-sorter", "", tempDirectory);
    }

}

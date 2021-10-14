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
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// is java.util.ArrayList but with an extra parallelSort method that is more memory efficient 
// that can be achieved outside the class
import com.github.davidmoten.bigsorter.internal.ArrayList;
import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

// NotThreadSafe
// The class is not considered thread safe because calling the sort() method on the same Sorter object simultaneously from 
// different threads could break things. Admittedly that would be a pretty strange thing to do! In short, create a new Sorter 
// and sort() in one thread, don't seek to reuse the same Sorter object. 
public final class Sorter<T> {

    private final FileSystem fs;
    private final Serializer<T> serializer;
    private final List<Supplier<? extends InputStream>> inputs;
    private final AbstractFile output;
    private final Comparator<? super T> comparator;
    private final int maxFilesPerMerge;
    private final int maxItemsPerPart;
    private final Consumer<? super String> log;
    private final int bufferSize;
    private final Function<? super Reader<T>, ? extends Reader<? extends T>> transform;
    private final boolean unique;
    private final boolean initialSortInParallel;
    private long count = 0;

    Sorter(FileSystem fs, Serializer<T> serializer,List<Supplier<? extends InputStream>> inputs, AbstractFile output,
            Comparator<? super T> comparator, int maxFilesPerMerge, int maxItemsPerFile,
            Consumer<? super String> log, int bufferSize,
            Function<? super Reader<T>, ? extends Reader<? extends T>> transform, boolean unique,
            boolean initialSortInParallel) {
        Preconditions.checkNotNull(fs, "fs cannot be null");
        Preconditions.checkNotNull(serializer, "serializer cannot be null");
        Preconditions.checkNotNull(output, "output cannot be null");
        Preconditions.checkNotNull(comparator, "comparator cannot be null");
        Preconditions.checkNotNull(transform, "transform cannot be null");
        this.fs = fs;
        this.serializer = serializer;
        this.inputs = inputs;
        this.output = output;
        this.comparator = comparator;
        this.maxFilesPerMerge = maxFilesPerMerge;
        this.maxItemsPerPart = maxItemsPerFile;
        this.log = log;
        this.bufferSize = bufferSize;
        this.transform = transform;
        this.unique = unique;
        this.initialSortInParallel = initialSortInParallel;
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
        private List<Supplier<? extends InputStream>> inputs = Lists.newArrayList();
        private final Serializer<T> serializer;
        private File output;
        private Comparator<? super T> comparator;
        private int maxFilesPerMerge = 100;
        private int maxItemsPerFile = 100000;
        private Consumer<? super String> logger = null;
        private int bufferSize = 8192;
        private File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        private Function<? super Reader<T>, ? extends Reader<? extends T>> transform = r -> r;
        private boolean unique;
        private boolean initialSortInParallel;

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

        public Builder3<T> input(Charset charset, String... strings) {
            Preconditions.checkNotNull(strings, "string cannot be null");
            Preconditions.checkNotNull(charset, "charset cannot be null");
            List<Supplier<InputStream>> list = Arrays //
                    .asList(strings) //
                    .stream() //
                    .map(string -> new ByteArrayInputStream(string.getBytes(charset))) //
                    .map(bis -> (Supplier<InputStream>) (() -> bis)) //
                    .collect(Collectors.toList());
            return inputStreams(list);
        }

        public Builder3<T> input(String... strings) {
            Preconditions.checkNotNull(strings);
            return input(StandardCharsets.UTF_8, strings);
        }

        public Builder3<T> input(InputStream... inputs) {
            List<Supplier<InputStream>> list = Lists.newArrayList();
            for (InputStream in : inputs) {
                list.add(() -> new NonClosingInputStream(in));
            }
            return inputStreams(list);
        }

        public Builder3<T> input(Supplier<? extends InputStream> input) {
            Preconditions.checkNotNull(input, "input cannot be null");
            return inputStreams(Collections.singletonList(input));
        }

        public Builder3<T> input(File... files) {
            return input(Arrays.asList(files));
        }

        public Builder3<T> input(List<File> files) {
            Preconditions.checkNotNull(files, "files cannot be null");
            return inputStreams(files //
                    .stream() //
                    .map(file -> supplier(new StandardFile(file))) //
                    .collect(Collectors.toList()));
        }

        public Builder3<T> inputStreams(List<? extends Supplier<? extends InputStream>> inputs) {
            Preconditions.checkNotNull(inputs);
            for (Supplier<? extends InputStream> input : inputs) {
                b.inputs.add(input);
            }
            return new Builder3<T>(b);
        }

        private Supplier<InputStream> supplier(AbstractFile file) {
            return () -> {
                try {
                    return openFile(file, b.bufferSize);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

    }

    public static final class Builder3<T> {
        private final Builder<T> b;

        Builder3(Builder<T> b) {
            this.b = b;
        }

        public Builder3<T> filter(Predicate<? super T> predicate) {
            Function<? super Reader<T>, ? extends Reader<? extends T>> currentTransform = b.transform;
            return transform(r -> currentTransform.apply(r).filter(predicate));
        }

        @SuppressWarnings("unchecked")
        public Builder3<T> map(Function<? super T, ? extends T> mapper) {
            Function<? super Reader<T>, ? extends Reader<? extends T>> currentTransform = b.transform;
            return transform(r -> ((Reader<T>) currentTransform.apply(r)).map(mapper));
        }

        @SuppressWarnings("unchecked")
        public Builder3<T> flatMap(Function<? super T, ? extends List<? extends T>> mapper) {
            Function<? super Reader<T>, ? extends Reader<? extends T>> currentTransform = b.transform;
            return transform(r -> ((Reader<T>) currentTransform.apply(r)).flatMap(mapper));
        }

        @SuppressWarnings("unchecked")
        public Builder3<T> transform(
                Function<? super Reader<T>, ? extends Reader<? extends T>> transform) {
            Preconditions.checkNotNull(transform, "transform cannot be null");
            Function<? super Reader<T>, ? extends Reader<? extends T>> currentTransform = b.transform;
            b.transform = r -> transform.apply((Reader<T>) currentTransform.apply(r));
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder3<T> transformStream(
                Function<? super Stream<T>, ? extends Stream<? extends T>> transform) {
            Preconditions.checkNotNull(transform, "transform cannot be null");
            Function<? super Reader<T>, ? extends Reader<? extends T>> currentTransform = b.transform;
            b.transform = r -> ((Reader<T>) currentTransform.apply(r)).transform(transform);
            return this;
        }

        public Builder4<T> output(File output) {
            Preconditions.checkNotNull(output, "output cannot be null");
            b.output = output;
            return new Builder4<T>(b);
        }

        public Builder5<T> outputAsStream() {
            return new Builder5<T>(b);
        }

    }

    public static class Builder4Base<T, S extends Builder4Base<T, S>> {
        protected final Builder<T> b;

        Builder4Base(Builder<T> b) {
            this.b = b;
        }

        @SuppressWarnings("unchecked")
        public S maxFilesPerMerge(int value) {
            Preconditions.checkArgument(value > 1, "maxFilesPerMerge must be greater than 1");
            b.maxFilesPerMerge = value;
            return (S) this;
        }

        /**
         * Sets the number of items in each file for the initial split. Default is
         * 100_000.
         * 
         * @param value the number of items in each file for the initial split
         * @return this
         */
        @SuppressWarnings("unchecked")
        public S maxItemsPerFile(int value) {
            Preconditions.checkArgument(value > 0, "maxItemsPerFile must be greater than 0");
            b.maxItemsPerFile = value;
            return (S) this;
        }

        @SuppressWarnings("unchecked")
        public S unique(boolean value) {
            b.unique = value;
            return (S) this;
        }

        public S unique() {
            return unique(true);
        }

        @SuppressWarnings("unchecked")
        public S initialSortInParallel(boolean initialSortInParallel) {
            b.initialSortInParallel = initialSortInParallel;
            return (S) this;
        }

        public S initialSortInParallel() {
            return initialSortInParallel(true);
        }

        @SuppressWarnings("unchecked")
        public S logger(Consumer<? super String> logger) {
            Preconditions.checkNotNull(logger, "logger cannot be null");
            b.logger = logger;
            return (S) this;
        }

        public S loggerStdOut() {
            return logger(new Consumer<String>() {

                @Override
                public void accept(String msg) {
                    System.out.println(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                            .format(Builder.DATE_TIME_PATTERN) + " " + msg);
                }
            });
        }

        @SuppressWarnings("unchecked")
        public S bufferSize(int bufferSize) {
            Preconditions.checkArgument(bufferSize > 0, "bufferSize must be greater than 0");
            b.bufferSize = bufferSize;
            return (S) this;
        }

        @SuppressWarnings("unchecked")
        public S tempDirectory(File directory) {
            Preconditions.checkNotNull(directory, "tempDirectory cannot be null");
            b.tempDirectory = directory;
            return (S) this;
        }

    }

    public static final class Builder4<T> extends Builder4Base<T, Builder4<T>> {

        Builder4(Builder<T> b) {
            super(b);
        }

        /**
         * Sorts the input and writes the result to the given output file. If an
         * {@link IOException} occurs then it is thrown wrapped in
         * {@link UncheckedIOException}.
         */
        public void sort() {
            FileSystem fs = new StandardFileSystem(b.tempDirectory);
            Sorter<T> sorter = new Sorter<T>(fs, b.serializer, b.inputs,output(fs, b.output), b.comparator,
                    b.maxFilesPerMerge, b.maxItemsPerFile, b.logger, b.bufferSize, b.transform,
                    b.unique, b.initialSortInParallel);
            try {
                sorter.sort();
            } catch (IOException e) {
                b.output.delete();
                throw new UncheckedIOException(e);
            }
        }

    }
    
    private static AbstractFile output(FileSystem fs, File output) {
        if (output != null) {
            return new StandardFile(output);
        } else {
            return fs.nextTempFile();
        }
    }

    public static final class Builder5<T> {

        private final Builder<T> b;

        Builder5(Builder<T> b) {
            this.b = b;
        }

        /**
         * Sorts the input and writes the result to the output file. The items in the
         * output file are returned as a Stream. The Stream must be closed (it is
         * AutoCloseable) to avoid consuming unnecessary disk space with many calls.
         * When the Stream is closed the output file is deleted. When a
         * {@link IOException} occurs it is thrown wrapped in a
         * {@link UncheckedIOException}.
         * 
         * <p>
         * Note that a terminal operation (like {@code .count()} for example) does NOT
         * close the Stream. You should assign the Stream to a variable in a
         * try-catch-with-resources block to ensure the output file is deleted.
         * 
         * @return stream that on close deletes the output file of the sort
         */
        public Stream<T> sort() {
            try {
                FileSystem fs = new StandardFileSystem(b.tempDirectory);
                AbstractFile output = output(fs, b.output);
                Sorter<T> sorter = new Sorter<T>(fs, b.serializer, b.inputs, output, b.comparator,
                        b.maxFilesPerMerge, b.maxItemsPerFile, b.logger, b.bufferSize, b.transform,
                        b.unique, b.initialSortInParallel);
                sorter.sort();
                return b.serializer //
                        .createReader(output) //
                        .stream() //
                        .onClose(() -> b.output.delete());
            } catch (Throwable e) {
                b.output.delete();
                throw Util.toRuntimeException(e);
            }
        }

    }

    static InputStream openFile(AbstractFile file, int bufferSize) throws IOException {
        return new BufferedInputStream(file.createInputStream(), bufferSize);
    }

    private void log(String msg, Object... objects) {
        if (log != null) {
            String s = String.format(msg, objects);
            log.accept(s);
        }
    }

    public interface FileSystem {
        void init();

        AbstractFile nextTempFile();
    }
    
    public interface AbstractFile {
        String name();

        void delete();
        
        AbstractFile sibling(String name);

        InputStream createInputStream() throws IOException;

        OutputStream createOutputStream() throws IOException;
    }

    public static final class StandardFileSystem implements FileSystem {

        private final File tempDirectory;

        public StandardFileSystem(File tempDirectory) {
            this.tempDirectory = tempDirectory;
        }

        @Override
        public void init() {
            tempDirectory.mkdirs();
        }

        @Override
        public AbstractFile nextTempFile() {
            try {
                return new StandardFile(Sorter.nextTempFile(tempDirectory));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    public static final class StandardFile implements AbstractFile {

        private final File file;

        public StandardFile(File file) {
            this.file = file;
        }

        @Override
        public String name() {
            return file.getName();
        }

        @Override
        public OutputStream createOutputStream() throws IOException {
            return new FileOutputStream(file);
        }

        @Override
        public InputStream createInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void delete() {
            file.delete();
        }

        @Override
        public AbstractFile sibling(String name) {
            return new StandardFile(new File(file.getParentFile(), name));
        }
        
        public File file() {
            return file;
        }

        public static StandardFile of(File file) {
            return new StandardFile(file);
        }

    }

    private AbstractFile sort() throws IOException {

        fs.init();

        // read the input into sorted small files
        long time = System.currentTimeMillis();
        count = 0;
        List<AbstractFile> files = new ArrayList<>();
        log("starting sort");
        log("unique = " + unique);

        int i = 0;
        ArrayList<T> list = new ArrayList<>();
        for (Supplier<? extends InputStream> supplier : inputs) {
            try (InputStream in = supplier.get();
                    Reader<? extends T> reader = transform.apply(serializer.createReader(in))) {
                while (true) {
                    T t = reader.read();
                    if (t != null) {
                        list.add(t);
                        i++;
                    }
                    if (t == null || i == maxItemsPerPart) {
                        i = 0;
                        if (list.size() > 0) {
                            AbstractFile f = sortAndWriteToFile(fs, list);
                            files.add(f);
                            list.clear();
                        }
                    }
                    if (t == null) {
                        break;
                    }
                }
            }
        }
        log("completed initial split and sort, starting merge, elapsed time="
                + (System.currentTimeMillis() - time) / 1000.0 + "s");
        merge(fs, files);
//        Files.move( //
//                result.toPath(), //
//                output.toPath(), //
//                StandardCopyOption.ATOMIC_MOVE, //
//                StandardCopyOption.REPLACE_EXISTING);
        log("sort of " + count + " records completed in "
                + (System.currentTimeMillis() - time) / 1000.0 + "s");
        return output;
    }

    @VisibleForTesting
    AbstractFile merge(FileSystem fs, List<AbstractFile> files) {
        // merge the files in chunks repeatededly until only one remains
        // TODO make a better guess at the chunk size so groups are more even
        try {
            while (files.size() > 1) {
                List<AbstractFile> nextRound = new ArrayList<>();
                AbstractFile mergeTo;
                if (files.size() <= maxFilesPerMerge) {
                    // is final merge
                    mergeTo = output;
                } else {
                    mergeTo = fs.nextTempFile();
                }
                for (int i = 0; i < files.size(); i += maxFilesPerMerge) {
                    AbstractFile merged = mergeGroup(fs,
                            files.subList(i, Math.min(files.size(), i + maxFilesPerMerge)),
                            mergeTo);
                    nextRound.add(merged);
                }
                files = nextRound;
            }
            if (files.isEmpty()) {
                output.createOutputStream().close();
            }
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AbstractFile mergeGroup(FileSystem fs, List<AbstractFile> list, AbstractFile output)
            throws IOException {
        log("merging %s files", list.size());
        if (list.size() == 1) {
            return list.get(0);
        }
        List<State<T>> states = new ArrayList<>();
        for (AbstractFile f : list) {
            State<T> st = createState(f);
            // note that st.value will be present otherwise the file would be empty
            // and an empty file would not be passed to this method
            states.add(st);
        }
        try (OutputStream out = new BufferedOutputStream(output.createOutputStream(), bufferSize);
                Writer<T> writer = serializer.createWriter(out)) {
            PriorityQueue<State<T>> q = new PriorityQueue<>(
                    (x, y) -> comparator.compare(x.value, y.value));
            q.addAll(states);
            T last = null;
            while (!q.isEmpty()) {
                State<T> state = q.poll();
                if (!unique || last == null || comparator.compare(state.value, last) != 0) {
                    writer.write(state.value);
                    last = state.value;
                }
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

    private State<T> createState(AbstractFile f) throws IOException {
        InputStream in = openFile(f, bufferSize);
        Reader<T> reader = serializer.createReader(in);
        T t = reader.readAutoClosing();
        return new State<T>(f, reader, t);
    }

    private static final class State<T> {
        final AbstractFile file;
        Reader<T> reader;
        T value;

        State(AbstractFile file, Reader<T> reader, T value) {
            this.file = file;
            this.reader = reader;
            this.value = value;
        }
    }

    private AbstractFile sortAndWriteToFile(FileSystem fs, ArrayList<T> list)
            throws FileNotFoundException, IOException {
        AbstractFile file = fs.nextTempFile();
        long t = System.currentTimeMillis();
        if (initialSortInParallel) {
            list.parallelSort(comparator);
        } else {
            list.sort(comparator);
        }
        writeToFile(list, file);
        DecimalFormat df = new DecimalFormat("0.000");
        count += list.size();
        log("total=%s, sorted %s records to file %s in %ss", //
                count, //
                list.size(), //
                file.name(), //
                df.format((System.currentTimeMillis() - t) / 1000.0));
        return file;
    }

    private void writeToFile(List<T> list, AbstractFile f)
            throws FileNotFoundException, IOException {
        try (OutputStream out = new BufferedOutputStream(f.createOutputStream(), bufferSize);
                Writer<T> writer = serializer.createWriter(out)) {
            T last = null;
            for (T t : list) {
                if (!unique || last == null || comparator.compare(t, last) != 0) {
                    writer.write(t);
                    last = t;
                }
            }
        }
    }

    private static File nextTempFile(File tempDirectory) throws IOException {
        return File.createTempFile("big-sorter", "", tempDirectory);
    }

}

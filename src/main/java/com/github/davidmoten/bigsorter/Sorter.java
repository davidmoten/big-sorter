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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
import com.github.davidmoten.bigsorter.internal.ReaderFromIterator;
import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

// NotThreadSafe
// The class is not considered thread safe because calling the sort() method on the same Sorter object simultaneously from 
// different threads could break things. Admittedly that would be a pretty strange thing to do! In short, create a new Sorter 
// and sort() in one thread, don't seek to reuse the same Sorter object. 
public final class Sorter<T> {

    private final List<Supplier<? extends Reader<? extends T>>> inputs;
    private final Serializer<T> serializer;
    private final File output;
    private final Comparator<? super T> comparator;
    private final int maxFilesPerMerge;
    private final int maxItemsPerPart;
    private final Consumer<? super String> log;
    private final int bufferSize;
    private final File tempDirectory;
    private final boolean unique;
    private final boolean initialSortInParallel;
    private final Optional<OutputStreamWriterFactory<T>> outputWriterFactory;
    private long count = 0;

    Sorter(List<Supplier<? extends Reader<? extends T>>> inputs, Serializer<T> serializer, File output,
            Comparator<? super T> comparator, int maxFilesPerMerge, int maxItemsPerFile, Consumer<? super String> log,
            int bufferSize, File tempDirectory, boolean unique, boolean initialSortInParallel, Optional<OutputStreamWriterFactory<T>> outputWriterFactory) {
        Preconditions.checkNotNull(inputs, "inputs cannot be null");
        Preconditions.checkNotNull(serializer, "serializer cannot be null");
        Preconditions.checkNotNull(output, "output cannot be null");
        Preconditions.checkNotNull(comparator, "comparator cannot be null");
        Preconditions.checkNotNull(outputWriterFactory, "outputWriterFactory cannot be null");
        this.inputs = inputs;
        this.serializer = serializer;
        this.output = output;
        this.comparator = comparator;
        this.maxFilesPerMerge = maxFilesPerMerge;
        this.maxItemsPerPart = maxItemsPerFile;
        this.log = log;
        this.bufferSize = bufferSize;
        this.tempDirectory = tempDirectory;
        this.unique = unique;
        this.initialSortInParallel = initialSortInParallel;
        this.outputWriterFactory = outputWriterFactory;
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
    
    private enum SourceType {
        SUPPLIER_INPUT_STREAM, SUPPLIER_READER
    }
    
    private static final class Source {
        final SourceType type;
        final Object source;

        Source(SourceType type, Object source) {
            this.type = type;
            this.source = source;
        }
    }

    public static final class Builder<T> {
        private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss.Sxxxx");
        private List<Source> inputs = Lists.newArrayList();
        private Optional<InputStreamReaderFactory<T>> inputReaderFactory = Optional.empty();
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
        private Optional<OutputStreamWriterFactory<T>> outputWriterFactory = Optional.empty();

        Builder(Serializer<T> serializer) {
            this.serializer = serializer;
        }
        
        /**
         * Sets a conversion for input before sorting with the main serializer happens.
         * Only applies when input is specified as a File or another supplier of an
         * InputStream.
         * 
         * @param <S>           input record type
         * @param readerFactory readerFactory for the input record type (can be a {@link Serializer})
         * @param mapper        conversion to <T>
         * @return this
         */
        public <S> Builder<T> inputMapper(InputStreamReaderFactory<? extends S> readerFactory, Function<? super S, ? extends T> mapper) {
            Preconditions.checkArgument(!inputReaderFactory.isPresent());
            InputStreamReaderFactory<T> factory = in -> readerFactory.createReader(in).map(mapper);
            this.inputReaderFactory = Optional.of(factory);
            return this;
        }
        
        public Builder2<T> comparator(Comparator<? super T> comparator) {
            Preconditions.checkNotNull(comparator, "comparator cannot be null");
            this.comparator = comparator;
            return new Builder2<T>(this);
        }
        
        @SuppressWarnings("unchecked")
        public Builder2<T> naturalOrder() {
            return comparator((Comparator<T>) Comparator.naturalOrder());
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
                    .map(bis -> (Supplier<InputStream>)(() -> bis)) //
                    .collect(Collectors.toList());
            return inputStreams(list);
        }

        public Builder3<T> input(String... strings) {
            Preconditions.checkNotNull(strings);
            return input(StandardCharsets.UTF_8, strings);
        }
        
        public Builder3<T> input(InputStream... inputs) {
            List<Supplier<InputStream>> list = Lists.newArrayList();
            for (InputStream in:inputs) {
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
                    
                    .map(file -> supplier(file)) //
                    .collect(Collectors.toList()));
        }
        
        public Builder3<T> inputStreams(List<? extends Supplier<? extends InputStream>> inputs) {
            Preconditions.checkNotNull(inputs);
            for (Supplier<? extends InputStream> input: inputs) {
                b.inputs.add(new Source(SourceType.SUPPLIER_INPUT_STREAM, input));
            }
            return new Builder3<T>(b);
        }
        
        public Builder3<T> readers(List<? extends Supplier<? extends Reader<? extends T>>> readers) {
            Preconditions.checkNotNull(readers);
            for (Supplier<? extends Reader<? extends T>> input: readers) {
                b.inputs.add(new Source(SourceType.SUPPLIER_READER, input));
            }
            return new Builder3<T>(b);
        }
        
        public Builder3<T> inputItems(@SuppressWarnings("unchecked") T... items) {
            return inputItems(Arrays.asList(items));
        }
        
        public Builder3<T> inputItems(Iterable<? extends T> iterable) {
            Supplier<? extends Reader<? extends T>> supplier = () -> new ReaderFromIterator<T>(iterable.iterator());
            return readers(Collections.singletonList(supplier));
        }
        
        public Builder3<T> inputItems(Iterator<? extends T> iterator) {
            Supplier<? extends Reader<? extends T>> supplier = () -> new ReaderFromIterator<T>(iterator);
            return readers(Collections.singletonList(supplier));
        }
        
        private Supplier<InputStream> supplier(File file) {
            return () -> {
                try {
                    return openFile(file, b.bufferSize);
                } catch (FileNotFoundException e) {
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
         * Sets the number of items in each file for the initial split. Default is 100_000.
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
        
        public <S> Builder4<T> outputMapper(OutputStreamWriterFactory<? super S> writerFactory, Function<? super T, ? extends S> mapper) {
            Preconditions.checkArgument(!b.outputWriterFactory.isPresent());
            OutputStreamWriterFactory<T> factory = out -> writerFactory.createWriter(out).map(mapper);
            b.outputWriterFactory  = Optional.of(factory);
            return this;
        }
        
        // TODO add flatMap method, stream transforms?

        /**
         * Sorts the input and writes the result to the given output file. If an
         * {@link IOException} occurs then it is thrown wrapped in
         * {@link UncheckedIOException}.
         */
        public void sort() {
            Sorter<T> sorter = new Sorter<T>(inputs(b), b.serializer, b.output, b.comparator,
                    b.maxFilesPerMerge, b.maxItemsPerFile, b.logger, b.bufferSize, b.tempDirectory,
                    b.unique, b.initialSortInParallel, b.outputWriterFactory);
            try {
                sorter.sort();
            } catch (IOException e) {
                b.output.delete();
                throw new UncheckedIOException(e);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private static <T> List<Supplier<? extends Reader<? extends T>>> inputs(Builder<T> b) {
        return b.inputs //
                .stream() //
                .map(source -> {
                    if (source.type == SourceType.SUPPLIER_INPUT_STREAM) {
                        return (Supplier<? extends Reader<? extends T>>) //
                        (() -> b.transform //
                                .apply(inputStreamReader(b, source)));
                    } else { // Supplier of a Reader
                        return (Supplier<? extends Reader<? extends T>>) 
                                (() -> b.transform //
                                        .apply(((Supplier<Reader<T>>) source.source).get()));
                    }
                }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> Reader<T> inputStreamReader(Builder<T> b, Source source) {
        InputStreamReaderFactory<T> rf = b.inputReaderFactory.orElse(b.serializer);
        return rf.createReader(((Supplier<? extends InputStream>) source.source).get());
    }
    
    public static final class Builder5<T> extends Builder4Base<T, Builder5<T>>{

        Builder5(Builder<T> b) {
            super(b);
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
                b.output = nextTempFile(b.tempDirectory);
                Sorter<T> sorter = new Sorter<T>(inputs(b), b.serializer, b.output, b.comparator,
                        b.maxFilesPerMerge, b.maxItemsPerFile, b.logger, b.bufferSize, b.tempDirectory,
                        b.unique, b.initialSortInParallel, b.outputWriterFactory);
                sorter.sort();
                return b.serializer //
                        .createReader(b.output) //
                        .stream() //
                        .onClose(() -> b.output.delete());
            } catch (Throwable e) {
                b.output.delete();
                throw Util.toRuntimeException(e);
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
    
    @FunctionalInterface
    private static interface CloseAction {
        void close() throws IOException;
    }
    
    ///////////////////////
    //
    // Main sort routine  
    //
    ///////////////////////
    
    private File sort() throws IOException {

        tempDirectory.mkdirs();

        // read the input into sorted small files
        long time = System.currentTimeMillis();
        count = 0;
        List<File> files = new ArrayList<>();
        log("starting sort");
        log("unique = " + unique);
        
        int i = 0;
        ArrayList<T> list = new ArrayList<>();
        for (Supplier<? extends Reader<? extends T>> supplier: inputs) {
            try (Reader<? extends T> reader = supplier.get()) {
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
        }
        log("completed initial split and sort, starting merge, elapsed time="
                + (System.currentTimeMillis() - time) / 1000.0 + "s");

        // TODO write final merge to final output to avoid possible copying at the end
        // (and apply outputWriterfactory on the fly if present)
        File result = merge(files);
        if (outputWriterFactory.isPresent()) {
            Util.convert(result, serializer, output, outputWriterFactory.get(), x -> x);
        } else {
            Files.move( //
                    result.toPath(), //
                    output.toPath(), //
                    StandardCopyOption.REPLACE_EXISTING);
        }
        log("sort of " + count + " records completed in "
                + (System.currentTimeMillis() - time) / 1000.0 + "s");
        return output;
    }

    @VisibleForTesting
    File merge(List<File> files) {
        // merge the files in chunks repeatedly until only one remains
        // TODO make a better guess at the chunk size so groups are more even
        try {
            while (files.size() > 1) {
                List<File> nextRound = new ArrayList<>();
                for (int i = 0; i < files.size(); i += maxFilesPerMerge) {
                    File merged = mergeGroup(
                            files.subList(i, Math.min(files.size(), i + maxFilesPerMerge)));
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

    private File sortAndWriteToFile(ArrayList<T> list) throws FileNotFoundException, IOException {
        File file = nextTempFile();
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
                file.getName(), //
                df.format((System.currentTimeMillis() - t) / 1000.0));
        return file;
    }

    private void writeToFile(List<T> list, File f) throws FileNotFoundException, IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f), bufferSize);
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

    private File nextTempFile() throws IOException {
        return nextTempFile(tempDirectory);
    }
    
    private static File nextTempFile(File tempDirectory) throws IOException {
        return Files.createTempFile(tempDirectory.toPath(), "big-sorter", "").toFile();
    }

}

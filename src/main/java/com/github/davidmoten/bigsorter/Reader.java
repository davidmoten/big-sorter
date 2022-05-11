package com.github.davidmoten.bigsorter;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Reader<T> extends Closeable, Iterable<T> {

    /**
     * Returns the next read value. If no more values returns null.
     * 
     * @return the next read value or null if no more values
     * @throws IOException on IO problem
     */
    T read() throws IOException;

    /**
     * Returns the next read value. If no more values close() is called then null
     * returned.
     * 
     * @return the next read value or null if no more values
     * @throws IOException on IO problem
     */
    default T readAutoClosing() throws IOException {
        T v = read();
        if (v == null) {
            close();
        }
        return v;
    }

    default Reader<T> filter(Predicate<? super T> predicate) {
        Reader<T> r = this;
        return new Reader<T>() {

            @Override
            public T read() throws IOException {
                T t = r.read();
                while (t != null && !predicate.test(t)) {
                    t = r.read();
                }
                return t;
            }

            @Override
            public void close() throws IOException {
                r.close();
            }
        };
    }

    default <S> Reader<S> map(Function<? super T, ? extends S> mapper) {
        Reader<T> r = this;
        return new Reader<S>() {

            @Override
            public S read() throws IOException {
                T v = r.read();
                if (v == null) {
                    return null;
                } else {
                    return mapper.apply(v);
                }
            }

            @Override
            public void close() throws IOException {
                r.close();
            }
        };
    }

    default Reader<T> flatMap(Function<? super T, ? extends List<? extends T>> mapper) {
        Reader<T> r = this;
        return new Reader<T>() {

            List<? extends T> list;
            int index;

            @Override
            public T read() throws IOException {
                while (list == null || index == list.size()) {
                    T t = r.read();
                    if (t == null) {
                        return null;
                    } else {
                        list = mapper.apply(t);
                        index = 0;
                    }
                }
                return list.get(index++);
            }

            @Override
            public void close() throws IOException {
                r.close();
            }
        };
    }

    default Reader<T> transform(
            Function<? super Stream<T>, ? extends Stream<? extends T>> function) {
        Stream<? extends T> s = function.apply(stream());
        return new Reader<T>() {

            Iterator<? extends T> it = s.iterator();

            @Override
            public T read() throws IOException {
                if (it.hasNext()) {
                    return it.next();
                } else {
                    return null;
                }
            }

            @Override
            public void close() throws IOException {
                s.close();
            }

        };
    }

    default Iterator<T> iterator() {
        return new Iterator<T>() {

            T t;

            @Override
            public boolean hasNext() {
                load();
                return t != null;
            }

            @Override
            public T next() {
                load();
                if (t == null) {
                    throw new NoSuchElementException();
                } else {
                    T v = t;
                    t = null;
                    return v;
                }
            }

            void load() {
                if (t == null) {
                    try {
                        t = read();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

        };
    }

    default Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false)
                .onClose(() -> Util.close(Reader.this));
    }
    
}

package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamReaderFactory<T> {

    Reader<T> createReader(InputStream in);

    default Reader<T> createReader(File file) throws FileNotFoundException {
        return createReader(new BufferedInputStream(new FileInputStream(file)));
    }
    
}

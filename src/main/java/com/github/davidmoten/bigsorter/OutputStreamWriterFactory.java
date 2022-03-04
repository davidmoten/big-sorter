package com.github.davidmoten.bigsorter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

@FunctionalInterface
public interface OutputStreamWriterFactory<T> {

    Writer<T> createWriter(OutputStream out);
    
    default Writer<T> createWriter(File file) throws FileNotFoundException {
        return createWriter(new BufferedOutputStream(new FileOutputStream(file)));
    }
    
}

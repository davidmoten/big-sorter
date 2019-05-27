package com.github.davidmoten.bigsorter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.github.davidmoten.guavamini.Preconditions;

public interface Serializer<T> {

    Reader<T> createReader(InputStream in);

    Writer<T> createWriter(OutputStream out);

    public static Serializer<String> linesUtf8() {
        return LinesSerializer.LINES_UTF8;
    }

    public static Serializer<String> lines(Charset charset) {
        Preconditions.checkNotNull(charset);
        return new LinesSerializer(charset);
    }

    public static <T extends Serializable> Serializer<T> java() {
        return JavaSerializer.instance();
    }

    public static Serializer<byte[]> fixedSizeRecord(int size) {
        Preconditions.checkArgument(size > 0);
        return new FixedSizeRecordSerializer(size);
    }

    public static Serializer<CSVRecord> csv(CSVFormat format, Charset charset) {
        Preconditions.checkNotNull(format, "format cannot be null");
        Preconditions.checkNotNull(charset, "charset cannot be null");
        return new CsvSerializer(format, charset);
    }
}

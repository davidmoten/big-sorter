package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.davidmoten.guavamini.Preconditions;

public interface Serializer<T> extends InputStreamReaderFactory<T>, OutputStreamWriterFactory<T> {

    static Serializer<String> linesUtf8() {
        return linesUtf8(LineDelimiter.LINE_FEED);
    }

    static Serializer<String> linesUtf8(LineDelimiter delimiter) {
        Preconditions.checkNotNull(delimiter);
        if (delimiter == LineDelimiter.LINE_FEED) {
            return LinesSerializer.LINES_UTF8_LF;
        } else {
            return LinesSerializer.LINES_UTF8_CR_LF;
        }
    }

    static Serializer<String> lines(Charset charset) {
        Preconditions.checkNotNull(charset);
        return new LinesSerializer(charset, LineDelimiter.LINE_FEED);
    }

    static Serializer<String> lines(Charset charset, LineDelimiter delimiter) {
        Preconditions.checkNotNull(charset);
        Preconditions.checkNotNull(delimiter);
        return new LinesSerializer(charset, delimiter);
    }

    static <T extends Serializable> Serializer<T> java() {
        return JavaSerializer.instance();
    }

    static Serializer<byte[]> fixedSizeRecord(int size) {
        Preconditions.checkArgument(size > 0);
        return new FixedSizeRecordSerializer(size);
    }

    static Serializer<CSVRecord> csv(CSVFormat format, Charset charset) {
        Preconditions.checkNotNull(format, "format cannot be null");
        Preconditions.checkNotNull(charset, "charset cannot be null");
        return new CsvSerializer(format, charset);
    }
    
    static Serializer<ObjectNode> jsonArray() {
        return JsonArraySerializer.INSTANCE;
    }
    
    static <T> Serializer<T> dataSerializer(Function<? super DataInputStream, ? extends T> reader, BiConsumer<? super DataOutputStream, ? super T> writer){
        return new DataSerializer<T>() {

            @Override
            public T read(DataInputStream dis) throws IOException {
                try {
                    return reader.apply(dis);
                } catch (EOFException e) {
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void write(DataOutputStream dos, T value) throws IOException {
                try {
                    writer.accept(dos, value);
                }  catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }};
    }
    
}

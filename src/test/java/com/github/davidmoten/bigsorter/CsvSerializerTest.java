package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

public class CsvSerializerTest {

    @Test
    public void testWriterEarlyClose() throws IOException {
        Serializer<CSVRecord> s = Serializer.csv(CSVFormat.DEFAULT, StandardCharsets.UTF_8);
        s.createWriter(System.out).close();
    }

    
    @Test
    public void testReaderEarlyClose() throws IOException {
        Serializer<CSVRecord> s = Serializer.csv(CSVFormat.DEFAULT, StandardCharsets.UTF_8);
        s.createReader(System.in).close();
    }
    
}

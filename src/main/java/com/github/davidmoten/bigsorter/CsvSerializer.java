package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

final class CsvSerializer implements Serializer<CSVRecord> {

    private final CSVFormat format;
    private final Charset charset;

    CsvSerializer(CSVFormat format, Charset charset) {
        this.format = format;
        this.charset = charset;
    }

    @Override
    public Reader<CSVRecord> createReader(InputStream in) {
        return new Reader<CSVRecord>() {

            Iterator<CSVRecord> it;
            InputStreamReader isr;

            @Override
            public CSVRecord read() throws IOException {
                if (it == null) {
                    isr = new InputStreamReader(in, charset);
                    it = format.parse(isr).iterator();
                }
                if (it.hasNext()) {
                    return it.next();
                } else {
                    return null;
                }
            }

            @Override
            public void close() throws IOException {
                if (isr != null) {
                    isr.close();
                }
            }
        };
    }

    @Override
    public Writer<CSVRecord> createWriter(OutputStream out) {
        return new Writer<CSVRecord>() {

            CSVPrinter printer;
            private PrintStream ps;

            @Override
            public void write(CSVRecord value) throws IOException {
                if (printer == null) {
                    ps = new PrintStream(out, false, charset.name());
                    printer = format.print(ps);
                    // print header line
                    List<String> h = value.getParser().getHeaderNames();
                    if (!h.isEmpty()) {
                        printer.printRecord(h);
                    }
                }
                printer.printRecord(value);
            }

            @Override
            public void close() throws IOException {
               flush();
            }

            @Override
            public void flush() {
                if (ps != null) {
                    ps.flush();
                }
            }

        };
    }
    
}

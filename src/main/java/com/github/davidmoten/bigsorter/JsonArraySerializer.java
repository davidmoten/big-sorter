package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class JsonArraySerializer implements Serializer<ObjectNode> {

    private final ObjectMapper mapper = new ObjectMapper();

    static final JsonArraySerializer INSTANCE = new JsonArraySerializer();

    private JsonArraySerializer() {
    }

    @Override
    public Reader<ObjectNode> createReader(InputStream in) {
        try {
            JsonParser parser = mapper.getFactory().createParser(in);
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array");
            }
            return new Reader<ObjectNode>() {

                @Override
                public ObjectNode read() throws IOException {
                    if (parser.nextToken() == JsonToken.START_OBJECT) {
                        // read everything from this START_OBJECT to the matching END_OBJECT
                        // and return it as a tree model ObjectNode
                        return mapper.readTree(parser);
                    } else {
                        // at end
                        parser.close();
                        return null;
                    }
                }

                @Override
                public void close() throws IOException {
                    parser.close();
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Writer<ObjectNode> createWriter(OutputStream out) {

        java.io.Writer w = new OutputStreamWriter(out);

        return new Writer<ObjectNode>() {

            boolean first = true;
            boolean closed = false;

            @Override
            public void write(ObjectNode node) throws IOException {
                if (first) {
                    w.write("[\n");
                    first = false;
                } else {
                    w.write(",\n");
                }
                w.write(mapper.writeValueAsString(node));
            }

            @Override
            public void flush() throws IOException {
                w.flush();
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    w.write("\n]");
                    w.close();
                    closed = true;
                }
            }
        };
    }
}

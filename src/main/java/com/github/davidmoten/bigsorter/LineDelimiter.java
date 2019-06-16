package com.github.davidmoten.bigsorter;

public enum LineDelimiter {

    LINE_FEED("\n"), //

    CARRIAGE_RETURN_LINE_FEED("\r\n");

    private final String delimiter;

    private LineDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String value() {
        return delimiter;
    }

}

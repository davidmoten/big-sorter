package com.github.davidmoten.bigsorter;

import java.util.List;
import java.util.TreeMap;

import org.davidmoten.hilbert.Range;

public class Index {

    private final int bits;
    private final int dimensions;
    private final double[] mins;
    private final double[] maxes;
    private final TreeMap<Integer, Long> indexPositions;

    public Index(int bits, int dimensions, double[] mins, double[] maxes,
            TreeMap<Integer, Long> indexPositions) {
        this.bits = bits;
        this.dimensions = dimensions;
        this.mins = mins;
        this.maxes = maxes;
        this.indexPositions = indexPositions;
    }

    /**
     * Fits the desired ranges to the effective querying ranges according to the
     * known index positions.
     * 
     * @param ranges list of ranges in ascending order
     * @return querying ranges based on known index positions
     */
    public List<Range> fit(List<Range> ranges) {
        return null;
    }

}

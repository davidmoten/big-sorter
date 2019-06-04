package com.github.davidmoten.bigsorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.Range;
import org.davidmoten.hilbert.SmallHilbertCurve;

public class Index {

    private final int bits;
    private final int dimensions;
    private final double[] mins;
    private final double[] maxes;
    private final TreeMap<Integer, Long> indexPositions;
    private final SmallHilbertCurve hc;

    public Index(int bits, int dimensions, double[] mins, double[] maxes, TreeMap<Integer, Long> indexPositions) {
        this.bits = bits;
        this.dimensions = dimensions;
        this.mins = mins;
        this.maxes = maxes;
        this.indexPositions = indexPositions;
        this.hc = HilbertCurve.small().bits(bits).dimensions(dimensions);
    }

    /**
     * Fits the desired ranges to the effective querying ranges according to the
     * known index positions.
     * 
     * @param ranges
     *            list of ranges in ascending order
     * @return querying ranges based on known index positions
     */
    public List<PositionRange> fit(List<Range> ranges) {
        List<PositionRange> list = new ArrayList<>();
        for (Range range : ranges) {
            Long startPosition = indexPositions.floorEntry((int) range.low()).getValue();
            if (startPosition == null) {
                startPosition = indexPositions.firstEntry().getValue();
            }
            Long endPosition = indexPositions.ceilingEntry((int) range.high()).getValue();
            if (endPosition == null) {
                endPosition = indexPositions.lastEntry().getValue();
            }
            list.add(new PositionRange(Collections.singletonList(range), startPosition, endPosition));
        }
        return simplify(list);
    }

    private static List<PositionRange> simplify(List<PositionRange> positionRanges) {
        LinkedList<PositionRange> list = new LinkedList<>();
        for (PositionRange p : positionRanges) {
            if (list.isEmpty()) {
                list.add(p);
            } else {
                PositionRange last = list.getLast();
                if (p.overlapsPositionWith(last)) {
                    list.pollLast();
                    list.offer(last.join(p));
                }
            }
        }
        return list;
    }

}

package com.github.davidmoten.bigsorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.davidmoten.hilbert.Range;

public final class Index {

    private final TreeMap<Integer, Long> indexPositions;

    Index(TreeMap<Integer, Long> indexPositions) {
        this.indexPositions = indexPositions;
    }

    /**
     * Fits the desired ranges to the effective querying ranges according to the
     * known index positions.
     * 
     * @param ranges
     *            list of ranges in ascending order
     * @return querying ranges based on known index positions
     */
    public List<PositionRange> getPositionRanges(List<Range> ranges) {
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
                if (last.overlapsPositionWith(p)) {
                    list.pollLast();
                    list.offer(last.join(p));
                }
            }
        }
        return list;
    }

}

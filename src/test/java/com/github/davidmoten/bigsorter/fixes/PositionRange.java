package com.github.davidmoten.bigsorter.fixes;

import java.util.ArrayList;
import java.util.List;

import org.davidmoten.hilbert.Range;

import com.github.davidmoten.guavamini.Preconditions;

public final class PositionRange {

    // hilbert curve index ranges covered by this position range
    private final List<Range> ranges;

    // highest known position with index less than or equal to lowIndex or the
    // lowest known position if nothing lower
    private final long floorPosition;

    // lowest known position with index less than or equal to highIndex or the
    // highest known position if nothing higher
    private final long ceilingPosition;

    public PositionRange(List<Range> ranges, long floorPosition, long ceilingPosition) {
        Preconditions.checkArgument(!ranges.isEmpty());
        this.ranges = ranges;
        this.floorPosition = floorPosition;
        this.ceilingPosition = ceilingPosition;
    }

    public List<Range> ranges() {
        return ranges;
    }

    public long floorPosition() {
        return floorPosition;
    }

    public long ceilingPosition() {
        return ceilingPosition;
    }

    public boolean overlapsPositionWith(PositionRange last) {
        return (floorPosition >= last.floorPosition && floorPosition <= last.ceilingPosition)
                || (ceilingPosition <= last.ceilingPosition && ceilingPosition >= last.floorPosition);
    }

    public PositionRange join(PositionRange other) {
        Preconditions.checkArgument(other.ranges.get(0).low() >= this.ranges.get(ranges.size() - 1).high());
        List<Range> ranges = new ArrayList<>();
        ranges.addAll(this.ranges);
        ranges.addAll(other.ranges);
        return new PositionRange(ranges, //
                Math.min(floorPosition, other.floorPosition), //
                Math.max(ceilingPosition, other.ceilingPosition));
    }

    public long highIndex() {
        return ranges.get(ranges.size() -1).high();
    }

    @Override
    public String toString() {
        return "PositionRange [ranges=" + ranges + ", floorPosition=" + floorPosition + ", ceilingPosition="
                + ceilingPosition + "]";
    }

}

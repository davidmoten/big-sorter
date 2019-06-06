package com.github.davidmoten.bigsorter.fixes;

import java.util.Arrays;

import com.github.davidmoten.guavamini.Preconditions;

public final class Bounds {

    private final double[] mins;
    private final double[] maxes;

    public Bounds(double[] a, double[] b) {
        Preconditions.checkArgument(a.length > 0);
        Preconditions.checkArgument(a.length == b.length);
        double[] mins = new double[a.length];
        double[] maxes = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            mins[i] = Math.min(a[i], b[i]);
            maxes[i] = Math.max(a[i], b[i]);
        }
        this.mins = mins;
        this.maxes = maxes;
    }

    public boolean contains(double[] d) {
        Preconditions.checkArgument(mins.length == d.length);
        for (int i = 0; i < mins.length; i++) {
            if (d[i] < mins[i] || d[i] > maxes[i]) {
                return false;
            }
        }
        return true;
    }

    public double[] mins() {
        return mins;
    }

    public double[] maxes() {
        return maxes;
    }

    @Override
    public String toString() {
        return "Bounds [mins=" + Arrays.toString(mins) + ", maxes=" + Arrays.toString(maxes) + "]";
    }

}

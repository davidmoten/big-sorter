package com.github.davidmoten.bigsorter.fixes;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.TreeMap;

import org.junit.Test;

public class IndexTest {

    @Test
    public void test() {
        TreeMap<Integer, Long> map = new TreeMap<>();
        map.put(237, 0L);
        map.put(472177237, 4082820L);
        Index index = new Index(map, new double[] { -85.14174, -115.24912, 1557868858000L },
                new double[] { 47.630283, 179.99948, 1557964800000L }, 10);
        long[] o = index.ordinates(-84.23007,-115.24912, 1557964123000L);
        System.out.println(Arrays.toString(o));
        assertEquals(153391853, index.hilbertCurve().index(o));
    }

}

package com.cmder.intensitysegments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntensitySegmentsTest {

    @Test
    void testExample1() {
        IntensitySegments segments = new IntensitySegments();
        assertEquals("[]", segments.toString());

        segments.add(10, 30, 1);
        assertEquals("[[10,1],[30,0]]", segments.toString());

        segments.add(20, 40, 1);
        assertEquals("[[10,1],[20,2],[30,1],[40,0]]", segments.toString());

        segments.add(10, 40, -2);
        assertEquals("[[10,-1],[20,0],[30,-1],[40,0]]", segments.toString());
    }

    @Test
    void testExample2() {
        IntensitySegments segments = new IntensitySegments();
        assertEquals("[]", segments.toString());

        segments.add(10, 30, 1);
        assertEquals("[[10,1],[30,0]]", segments.toString());

        segments.add(20, 40, 1);
        assertEquals("[[10,1],[20,2],[30,1],[40,0]]", segments.toString());

        segments.add(10, 40, -1);
        assertEquals("[[20,1],[30,0]]", segments.toString());

        segments.add(10, 40, -1);
        assertEquals("[[10,-1],[20,0],[30,-1],[40,0]]", segments.toString());
    }

    @Test
    void testSetOperations() {
        IntensitySegments segments = new IntensitySegments();
        segments.add(10, 30, 1);
        assertEquals("[[10,1],[30,0]]", segments.toString());

        segments.set(15, 25, 5);
        assertEquals("[[10,1],[15,5],[25,1],[30,0]]", segments.toString());

        segments.set(20, 40, 3);
        assertEquals("[[10,1],[15,5],[20,3],[40,0]]", segments.toString());
    }

    @Test
    void testEdgeCases() {
        IntensitySegments segments = new IntensitySegments();

        // Empty add/set
        segments.add(10, 10, 1);
        assertEquals("[]", segments.toString());
        segments.set(10, 10, 1);
        assertEquals("[]", segments.toString());

        // Negative ranges
        segments.add(-10, 0, 2);
        assertEquals("[[-10,2],[0,0]]", segments.toString());

        // Overlapping and zero
        segments.add(-20, 10, -2);
        assertEquals("[[-20,-2],[-10,0],[0,-2],[10,0]]", segments.toString());

        // Set to zero
        segments.set(-15, 5, 0);
        assertEquals("[[-20,-2],[-15,0],[5,-2],[10,0]]", segments.toString());

        // All zero after operations
        segments.add(-20, 10, 2);
        assertEquals("[]", segments.toString());
    }

    @Test
    void testLargeRanges() {
        IntensitySegments segments = new IntensitySegments();
        segments.add(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, 1);
        // Not testing output due to large, but ensures no crash
        segments.add(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, -1);
        assertEquals("[]", segments.toString());
    }
}

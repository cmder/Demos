package com.cmder.intensitysegments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manages intensity values over intervals from -infinity to +infinity.
 * Intensity starts at 0 everywhere.
 * Uses a TreeMap to store start points and their intensity values for efficient operations.
 * Each entry [start, value] means the intensity is 'value' from 'start' to the next start (half-open).
 */
public class IntensitySegments {

    private final TreeMap<Integer, Integer> map = new TreeMap<>();

    /**
     * Adds the given amount to the intensity in the range [from, to).
     *
     * @param from   start of the range (inclusive)
     * @param to     end of the range (exclusive)
     * @param amount integer to add
     */
    public void add(int from, int to, int amount) {
        if (from >= to || amount == 0) {
            return;
        }
        // Compute original value at 'to' before updates
        Map.Entry<Integer, Integer> floorEntry = map.floorEntry(to - 1);
        int originalToValue = floorEntry != null ? floorEntry.getValue() : 0;

        // Split at 'from' if necessary
        Integer ceilingKey = map.ceilingKey(from);
        if (ceilingKey == null || ceilingKey > from) {
            Integer prevKey = map.floorKey(from - 1);
            int prevValue = prevKey != null ? map.get(prevKey) : 0;
            map.put(from, prevValue);
        }

        // Add amount to all segments in [from, to)
        Iterator<Map.Entry<Integer, Integer>> iterator = map.tailMap(from, true).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            if (entry.getKey() >= to) {
                break;
            }
            entry.setValue(entry.getValue() + amount);
        }

        // Split at 'to' if necessary
        if (!map.containsKey(to)) {
            Integer prevKey = map.floorKey(to - 1);
            if (prevKey != null && prevKey < to) {
                map.put(to, originalToValue);
            }
        }

        merge();
    }

    /**
     * Sets the intensity to the given amount in the range [from, to).
     *
     * @param from   start of the range (inclusive)
     * @param to     end of the range (exclusive)
     * @param amount integer to set
     */
    public void set(int from, int to, int amount) {
        if (from >= to) {
            return;
        }
        // Compute original value at 'to' before updates
        Map.Entry<Integer, Integer> floorEntry = map.floorEntry(to - 1);
        int originalToValue = floorEntry != null ? floorEntry.getValue() : 0;

        // Remove existing segments in [from, to)
        map.subMap(from, true, to, false).clear();

        // Set at 'from'
        map.put(from, amount);

        // Split at 'to' if necessary
        if (!map.containsKey(to)) {
            Integer prevKey = map.floorKey(to - 1);
            if (prevKey != null) {
                map.put(to, originalToValue);
            }
        }

        merge();
    }

    /**
     * Returns a string representation of the non-zero intensity segments.
     * Format: "[[start1,value1],[start2,value2],...]"
     * Only includes relevant change points, ending with 0 if necessary.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        if (map.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("[").append(entry.getKey()).append(",").append(entry.getValue()).append("]");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Merges adjacent segments with the same value and removes leading zeros.
     * Time: O(n) where n is number of entries.
     */
    private void merge() {
        if (map.isEmpty()) {
            return;
        }
        List<Integer> toRemove = new ArrayList<>();
        Map.Entry<Integer, Integer> prev = null;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (prev != null && prev.getValue().equals(entry.getValue())) {
                toRemove.add(entry.getKey());
            } else {
                prev = entry;
            }
        }
        for (Integer key : toRemove) {
            map.remove(key);
        }
        // Remove leading zeros
        while (!map.isEmpty() && map.firstEntry().getValue() == 0) {
            map.pollFirstEntry();
        }
    }
}
# Intensity Segments

This project implements a class to manage intensity values over infinite intervals, supporting add and set operations on ranges, with a toString method for representation.

## Features
- Efficient range updates (add/set) with merging of adjacent segments.
- Time complexity: O(log n) for lookups/inserts + O(n) for merging per operation (n = number of segments).
- Handles edge cases like empty ranges, negative values, infinite extents.
- Production-quality code: clean, documented, testable.

## Building and Running
This is a Maven project.

- **Build**: `mvn clean install`
- **Run tests**: `mvn test`
- **Requirements**: Java 11+, Maven 3.6+

## Design
- Uses `TreeMap<Integer, Integer>` to store start points and intensity values, ensuring sorted order.
- For `add` and `set`: split ranges if needed, update values, then merge adjacent same-value segments and remove leading zeros.
- `toString`: Outputs change points in the specified format, skipping redundant zeros.

## Usage Example
See `IntensitySegmentsTest.java` for examples.

## License
MIT
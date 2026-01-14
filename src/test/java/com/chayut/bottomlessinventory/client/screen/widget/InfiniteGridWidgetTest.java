package com.chayut.bottomlessinventory.client.screen.widget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InfiniteGridWidget functionality.
 * Note: Full widget rendering requires a Minecraft client environment.
 * These tests focus on the static utility methods that can be tested in isolation.
 */
class InfiniteGridWidgetTest {

    // === Count Abbreviation Tests ===

    @Test
    void abbreviateCount_smallNumbers_returnsExactString() {
        assertEquals("0", InfiniteGridWidget.abbreviateCount(0));
        assertEquals("1", InfiniteGridWidget.abbreviateCount(1));
        assertEquals("64", InfiniteGridWidget.abbreviateCount(64));
        assertEquals("999", InfiniteGridWidget.abbreviateCount(999));
    }

    @ParameterizedTest
    @CsvSource({
            "1000, 1K",
            "1500, 1.5K",
            "10000, 10K",
            "15000, 15K",
            "99999, 100K",
            "100000, 100K",
            "999999, 1000K"
    })
    void abbreviateCount_thousands_returnsKSuffix(long input, String expected) {
        assertEquals(expected, InfiniteGridWidget.abbreviateCount(input));
    }

    @ParameterizedTest
    @CsvSource({
            "1000000, 1M",
            "1500000, 1.5M",
            "10000000, 10M",
            "15000000, 15M",
            "99999999, 100M",
            "100000000, 100M",
            "999999999, 1000M"
    })
    void abbreviateCount_millions_returnsMSuffix(long input, String expected) {
        assertEquals(expected, InfiniteGridWidget.abbreviateCount(input));
    }

    @ParameterizedTest
    @CsvSource({
            "1000000000, 1B",
            "1500000000, 1.5B",
            "10000000000, 10B",
            "15000000000, 15B",
            "999999999999, 1000B"
    })
    void abbreviateCount_billions_returnsBSuffix(long input, String expected) {
        assertEquals(expected, InfiniteGridWidget.abbreviateCount(input));
    }

    @Test
    void abbreviateCount_veryLargeNumber_handlesBillions() {
        // Test with Long.MAX_VALUE
        String result = InfiniteGridWidget.abbreviateCount(Long.MAX_VALUE);
        assertTrue(result.endsWith("B"), "Should use billions suffix for very large numbers");
    }

    @Test
    void abbreviateCount_edgeCases_roundsCorrectly() {
        // Test boundary cases
        assertEquals("999", InfiniteGridWidget.abbreviateCount(999));
        assertEquals("1K", InfiniteGridWidget.abbreviateCount(1000));

        assertEquals("999.9K", InfiniteGridWidget.abbreviateCount(999_900));
        assertEquals("1M", InfiniteGridWidget.abbreviateCount(1_000_000));

        assertEquals("999.9M", InfiniteGridWidget.abbreviateCount(999_900_000));
        assertEquals("1B", InfiniteGridWidget.abbreviateCount(1_000_000_000));
    }

    @Test
    void abbreviateCount_removesTrailingZeroDecimal() {
        // When the decimal is .0, it should be removed
        assertEquals("1K", InfiniteGridWidget.abbreviateCount(1000));
        assertEquals("10K", InfiniteGridWidget.abbreviateCount(10000));
        assertEquals("1M", InfiniteGridWidget.abbreviateCount(1000000));
        assertEquals("10M", InfiniteGridWidget.abbreviateCount(10000000));
        assertEquals("1B", InfiniteGridWidget.abbreviateCount(1000000000));
        assertEquals("10B", InfiniteGridWidget.abbreviateCount(10000000000L));
    }

    @Test
    void abbreviateCount_keepsNonZeroDecimal() {
        // When the decimal is not .0, it should be kept
        assertEquals("1.5K", InfiniteGridWidget.abbreviateCount(1500));
        assertEquals("2.3K", InfiniteGridWidget.abbreviateCount(2300));
        assertEquals("1.5M", InfiniteGridWidget.abbreviateCount(1500000));
        assertEquals("2.3M", InfiniteGridWidget.abbreviateCount(2300000));
        assertEquals("1.5B", InfiniteGridWidget.abbreviateCount(1500000000));
        assertEquals("2.3B", InfiniteGridWidget.abbreviateCount(2300000000L));
    }

    @Test
    void abbreviateCount_negativeNumbers_handlesCorrectly() {
        // While counts should never be negative in practice,
        // the method returns them as-is (no abbreviation for negative numbers
        // because >= checks don't apply)
        assertEquals("-1", InfiniteGridWidget.abbreviateCount(-1));
        assertEquals("-1000", InfiniteGridWidget.abbreviateCount(-1000));
        assertEquals("-1000000", InfiniteGridWidget.abbreviateCount(-1000000));
        assertEquals("-1000000000", InfiniteGridWidget.abbreviateCount(-1000000000));
    }

    // === Widget Calculation Tests ===
    // These would require full Minecraft client setup, so are commented out
    // but included here for reference:

    /*
    @Test
    void widget_getVisibleRows_calculatesCorrectly() {
        InfiniteGridWidget widget = new InfiniteGridWidget(0, 0, 162, 126);
        // With CELL_SIZE = 18, height 126 should give 7 rows
        assertEquals(7, widget.getVisibleRows());
    }

    @Test
    void widget_getTotalRows_calculatesCorrectly() {
        // 9 columns, so:
        // 0-9 items = 1 row
        // 10-18 items = 2 rows
        // 19-27 items = 3 rows
        assertEquals(1, getTotalRows(9));
        assertEquals(2, getTotalRows(10));
        assertEquals(2, getTotalRows(18));
        assertEquals(3, getTotalRows(19));
    }
    */
}

package com.stikr.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BatchPlannerTest {
    @Test fun sizesFor100() { assertEquals(listOf(30, 30, 30, 10), BatchPlanner.chunkSizes(100)) }
    @Test fun sizesFor45() { assertEquals(listOf(30, 15), BatchPlanner.chunkSizes(45)) }
    @Test fun sizesFor30() { assertEquals(listOf(30), BatchPlanner.chunkSizes(30)) }
    @Test fun sizesFor31() { assertEquals(listOf(30, 1), BatchPlanner.chunkSizes(31)) }
    @Test fun sizesFor1() { assertEquals(listOf(1), BatchPlanner.chunkSizes(1)) }
    @Test fun sizesForZeroOrLess() { assertEquals(emptyList<Int>(), BatchPlanner.chunkSizes(0)); assertEquals(emptyList<Int>(), BatchPlanner.chunkSizes(-4)) }

    @Test fun plainNameForSinglePack() { assertEquals("Beach trip", BatchPlanner.packName("Beach trip", 0, 1)) }
    @Test fun numberedNamesForSplits() {
        assertEquals("Beach trip 1", BatchPlanner.packName("Beach trip", 0, 4))
        assertEquals("Beach trip 4", BatchPlanner.packName("Beach trip", 3, 4))
    }

    @Test fun nextNameAppendsTwo() { assertEquals("My stickers 2", BatchPlanner.nextPackName("My stickers")) }
    @Test fun nextNameIncrementsTrailingNumber() {
        assertEquals("My stickers 3", BatchPlanner.nextPackName("My stickers 2"))
        assertEquals("Batch 11", BatchPlanner.nextPackName("Batch 10"))
    }
    @Test fun nextNameWithDigitsOnly() { assertEquals("123 2", BatchPlanner.nextPackName("123")) }
}

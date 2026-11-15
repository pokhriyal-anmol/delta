package dev.anpo.f1.learn

private val TODAY = intArrayOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)

/** Points on today's 25-18-15-12-10-8-6-4-2-1 table for finishing position texts. R, D, W and anything outside the top ten score zero. */
fun rescore(positions: List<String>): Int =
    positions.sumOf { p -> p.toIntOrNull()?.let { TODAY.getOrNull(it - 1) } ?: 0 }

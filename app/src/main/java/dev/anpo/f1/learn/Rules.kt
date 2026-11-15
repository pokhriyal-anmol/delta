package dev.anpo.f1.learn

private const val MAX_PLACES_TO_SERVE = 15

/**
 * Grid slot after a grid penalty, FIA-B B2.5.4b.
 * shortcut: models one penalised car only; the multi-car swap procedure is not built because no screen needs it.
 */
fun gridAfterPenalty(qualified: Int, places: Int, field: Int): Int =
    if (places > MAX_PLACES_TO_SERVE) field else minOf(qualified + places, field)

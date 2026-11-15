package dev.anpo.f1.learn

/** Cars eliminated after Q1 and after Q2. Q3 always has 10 cars (FIA qualifying note: 20 -> 5,5; 22 -> 6,6; 24 -> 7,7). */
fun knockout(cars: Int): List<Int> {
    val q1 = ((cars - 10) / 2).coerceAtLeast(0)
    val q2 = (cars - q1 - 10).coerceAtLeast(0)
    return listOf(q1, q2)
}

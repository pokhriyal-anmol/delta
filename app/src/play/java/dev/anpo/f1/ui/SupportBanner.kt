package dev.anpo.f1.ui

import androidx.compose.runtime.Composable

/**
 * Deliberately empty. Google Play's payments policy has removed apps for donation links, so the
 * Play build carries no banner, no support URL and no copy about donating. The real one lives in
 * `src/direct` and is never compiled into this flavor.
 */
@Composable
fun SupportBanner() = Unit

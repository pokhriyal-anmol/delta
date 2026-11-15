package dev.anpo.f1.data

const val RELEASES_FEED = "https://github.com/pokhriyal-anmol/delta/releases.atom"
const val RELEASE_PAGE = "https://github.com/pokhriyal-anmol/delta/releases/tag/"

private val RELEASE_TAG = Regex("""/releases/tag/(v\d+(?:\.\d+){1,3})""")

/** The newest release tag in GitHub's releases feed. Only a plain vX.Y[.Z] tag is accepted, so nothing else from the feed can become a link. */
fun latestTag(atom: String): String? = RELEASE_TAG.find(atom)?.groupValues?.get(1)

private fun parts(v: String): List<Int>? = v.removePrefix("v").split(".").map { it.toIntOrNull() ?: return null }

/** True when tag ("v1.4.0") is a higher version than current ("1.3.2"). An unreadable version is never newer. */
fun isNewer(tag: String, current: String): Boolean {
    val a = parts(tag) ?: return false
    val b = parts(current) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

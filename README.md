# F1 Tracker — Android

The F1 tab, real. Jolpica for data, F1's CDN for headshots, the design system we settled on.
Three of the ~26 dev-days in the V1 plan.

## Build it

**No APK is attached, and here's exactly why:** the sandbox I work in is `aarch64`. Google ships
`aapt2` — the Android resource compiler — as **x86-64 only**. There is no ARM Linux build. `d8`
and `apksigner` run on the JVM and are fine; `aapt2` simply cannot execute. I tried emulating it
via `qemu-user-static` and the package download kept truncating. Two blockers stacked, so I
stopped rather than burn an hour and still fail.

On your machine the toolchain is native and this takes about 90 seconds.

```bash
# Android Studio: File > Open > this folder > Run.
# Or headless (needs JDK 17 and ANDROID_HOME set):
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Sideload:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a release build you need a signing key:
```bash
keytool -genkey -v -keystore ~/f1.jks -keyalg RSA -keysize 4096 -validity 10000 -alias f1
# add signingConfigs to app/build.gradle.kts, then:
./gradlew assembleRelease
```

## What works

- **Home** — next race pulled from the real calendar, live countdown, driver standings, constructor tiles
- **Standings** — full driver table, tap through to any driver
- **Driver** — carbon hero number, team-colour glow, real headshot in original colour
- **Cache** — Room (`feeds.db`), daily TTL on standings, weekly on the calendar
- **Rate limiter** — token bucket, persisted across process death
- **Design system** — the whole thing as Compose tokens in `ui/Theme.kt`

## What doesn't (yet)

- Race weekend, race result, calendar, constructor page, head-to-head — designed in
  `f1-tab-complete.html`, not ported
- The 3D circuit — needs the MultiViewer fetch + a Compose `Path`. `f1-tab-complete.html` has
  the real Spa geometry to lift
- Tasks and Valorant tabs — nav icons are inert
- **Untested.** I have no emulator or device here. Expect it to need a fix or two on first run.
  Most likely spot: `CarbonNumber` uses `getPathForRange`, which is fussy across Compose versions.

## Things in here that are decisions, not defaults

**`RateLimiter` is persisted, not in-memory.** Jolpica allows 200 req/hour. An in-memory limiter
resets on every process death — which is exactly when you'd blow the quota. It'll bite you in
development long before production: hot-reloading a screen that fetches on composition burns
through 200 in minutes.

**Two databases, never one.** `feeds.db` is a disposable cache excluded from backup. When
`tasks.db` arrives it's the sacred one, with its own lifecycle and backup rules. Mixing them is
how you end up restoring a stale cache over real data.

**`teamColour` and `headshotKey` are hardcoded maps, not fetches.** Both live in OpenF1 — and
OpenF1's free tier goes **completely dark during live sessions**, historical included. Your F1
tab would break every race weekend. Eleven teams and twenty-two drivers change once a year; a
constant is more reliable than a request. (The headshot path isn't derivable anyway: Antonelli
sits under `K/` but is coded `ANDANT01`.)

**`accentFor()` in `Theme.kt`.** Ferrari `#ED1131` and Audi `#F50537` are both within ~30° of
Nothing Red `#D71921`. Adjacent, they read as a rendering bug. On a near-red team's page, chrome
falls back to monochrome and lets the team own the colour.

**Errors never clear the cache.** A failed refresh shows "Showing cached data · <reason>". Stale
beats empty; a spinner that never resolves is the worst outcome.

## Files worth reading first

| File | Why |
|---|---|
| `SECURITY.md` | Why "hackers scanning the internet" can't reach this, and what's actually worth doing |
| `ui/Theme.kt` | The design system. Every value was a decision. |
| `data/RateLimiter.kt` | The thing that stops you getting 429'd on day one |
| `data/Repo.kt` | Cache TTLs and the OpenF1 workarounds |

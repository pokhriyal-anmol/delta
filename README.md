# Delta

A Formula 1 tracker for Android. Standings, drivers, teams, the race calendar and a 3D track,
from free public data. Sideloaded, one user, no server, no background work.

Latest release: v1.1.2. V2 (three tabs: Home, Championship, News) is in progress and
planned in `.scratch/f1-v2/spec.md`; slices S0 to S4 are done, unreleased.

## Build it

```bash
./gradlew assembleDebug      # needs JDK 17 and ANDROID_HOME
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds are signed from `keystore.properties` (copy `keystore.properties.example`) and
published with `./release.sh vX.Y.Z`. A release build refuses to run without the keystore.

## What works today

- Home: next race from the real calendar with a live countdown, driver standings, team tiles, the next circuit in 3D
- Championship: Drivers and Constructors tables with a season selector back to 1950 and a champion marker; tap a row to open the driver or team
- Driver: carbon race number, team-colour glow, real headshot, season stats, points by round, a link to the team, career totals, teammate head-to-head, recent results and season history
- Constructor: both drivers, season stats, team facts, points by round, career totals and season history
- Calendar: every round of the season; tap a round for its result, its weekend or its circuit
- Race result: the full classification for any finished round, laps down and retirements included
- Circuit: the 3D track, derived length, laps, turns and the winner of every race held there
- Race weekend: track, derived length, laps, turns and local-time sessions for any circuit that has an outline
- Three tabs, each with its own back stack: Home, Championship, News (a placeholder)
- Race reminder: one notification an hour before lights out. The line under the countdown on Home says "reminder on" or "reminder off" and a tap toggles it
- After the final race Home shows "Season complete" instead of a countdown
- Light and dark follow the phone

## What does not work yet

All of this is in the V2 spec with the evidence behind it.

- Race results exist for the current season only, and a retired driver in an old table does not open
- News is a placeholder until the feed list lands

## Using the emulator from the Claude desktop app

The desktop app streams iOS simulators only. For the Android emulator, `tools/emu-mirror.py` serves a
live screen with tap, swipe, Back, Home, theme and "Install latest build" buttons on localhost:

```bash
python3 tools/emu-mirror.py     # then open http://127.0.0.1:4391 in the browser pane
```

## Decisions that are decisions, not defaults

**`RateLimiter` is persisted, not in-memory.** Jolpica allows 200 requests an hour. An in-memory
limiter resets on every process death, which is exactly when you would blow the quota.

**`teamColour` and `headshotKey` are hardcoded maps, not fetches.** Both live in OpenF1, which
goes completely dark during live sessions. Eleven teams and twenty-two drivers change once a
year, so a constant is more reliable than a request.

**`accentFor()` in `Theme.kt`.** Ferrari and Audi reds are within about 30 degrees of the app's
red accent. Side by side they read as a rendering bug, so on a near-red team's page the chrome
falls back to monochrome and the team owns the colour.

**Errors never clear the cache.** A failed refresh shows "Showing cached data" and the reason.
Stale beats empty.

## Files worth reading first

| File | Why |
|---|---|
| `CLAUDE.md` | Rules, verified API facts, current state |
| `.scratch/f1-v2/spec.md` | The plan for V2 |
| `design-system.md` | Every visual decision and why |
| `SECURITY.md` | Why the internet cannot reach this app, and the News rules |
| `ui/Theme.kt` | The design system as code |
| `data/Repo.kt` | Cache lifetimes and the pinned maps |

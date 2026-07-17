# CLAUDE.md — context for this repo

Personal tracker. Three tabs: Tasks, Valorant, F1. Android, Kotlin, Compose. Sideloaded,
single user, no Play Store. **F1 tab only is implemented** — ~3 of a ~26 dev-day V1.

## Read first

| File | What it is |
|---|---|
| `../design-system.md` | Every visual decision + *why*. Don't re-derive; don't drift. |
| `../feature-map.md` | ~90 features with IDs (`F-01`, `T-30`, `V-22`). Use the IDs. |
| `../personal-tracker-architecture.md` | Data sources, sync design, build order, risks |
| `../f1-tab-complete.html` | **The visual spec.** Open in a browser. Screens not yet ported live here — including real Spa geometry. |
| `SECURITY.md` | Threat model. Read before touching anything network. |

## Non-negotiables

**No background execution. Ever.** User constraint, stated flatly. No polling, no WorkManager
periodic, no foreground services, no persistent sockets. The app does nothing when closed.
- `AlarmManager` for *known future times* (race start, task due) is fine — the OS wakes you, you
  do no work. That's not a loophole; it's the distinction that matters.
- Discovering unknown state ("is X live now?") requires polling → **banned**. Twitch live
  notifications are cut (`V-26`). The only route is an external push relay (`V-27`, deferred).

**Local-first, no server.** Task data lives on the devices and syncs peer-to-peer via Syncthing.
There is no cloud, and the absence of a server is a security property (see `SECURITY.md`).

**Two databases, never one.** `feeds.db` = disposable cache, excluded from backup, delete freely.
`tasks.db` = sacred, syncs, never mixed with the cache.

## API facts (verified July 2026 — do not re-guess these)

**Jolpica** `api.jolpi.ca/ergast/f1/` — free, no auth, 1950→now, **200 req/HOUR**. Load-bearing.
Volunteer-run, break-even goal — cache like it might vanish.

**OpenF1 goes COMPLETELY DARK during live sessions.** Not degraded — locked, historical included:
```
{"detail":"Live F1 session in progress. Global API access (including past sessions)
 is restricted to authenticated users until the session ends."}
```
Never put anything on the race-Sunday path behind OpenF1. This is why `teamColour()` and
`headshotKey()` in `Repo.kt` are **hardcoded maps, not fetches** — don't "improve" them into
API calls.

**MultiViewer** `api.multiviewer.app/api/v1/circuits/{key}/{year}` — free, no auth, real geometry
for all 22 tracks. Spa is key `7`. 1005 points, 19 corners, `rotation` in degrees.

**F1 CDN headshots** — `media.formula1.com/content/dam/fom-website/drivers/{key}.png`. Transparent
PNG. Path is **not derivable** from `driverId` (Antonelli → `K/ANDANT01_Kimi_Antonelli/andant01`).

**Live F1 timing is free** via SignalR at `livetiming.formula1.com` — the feed OpenF1 resells.
Classic SignalR 1.5: negotiate → wss → Subscribe. **Headers are case-sensitive** (`User-Agent:
BestHTTP`); wrong case → 500. 16 streams. Foreground-only, so it doesn't violate the no-background
rule. See architecture §4.1.

**Car liveries don't exist.** F1 publishes no 2026 car art. Don't go looking again.

## Design rules that will get broken by accident

- **No type between 23sp and 38sp.** The gap is the design.
- **Team colour is DATA. Never chrome.** `accentFor()` handles the Ferrari/Audi red collision.
- **Depth on `#050506` comes from light, not shadow.** Inset hairlines + backdrop blur. A drop
  shadow on near-black is invisible — this has already caused two "it's missing" bugs.
- **Never `mix-blend-mode` on anything with alpha.** It blends the whole box. Use SVG filters.
- Canvas is `#050506` not `#000` (shadows need headroom). Light canvas `#F5F5F3`, cards white.

## Current state

Working: Home (real calendar + countdown + standings), Standings, Driver (carbon number, team
glow, real headshot in original colour), Room cache, persisted rate limiter, design tokens.

**Untested — no device was available when it was written.** Likeliest breakage: `CarbonNumber` in
`ui/Components.kt` uses `getPathForRange`, which is fussy across Compose versions.

Not ported (all designed in `f1-tab-complete.html`): race weekend `F-01`, race result `F-04`,
calendar `F-09`, constructor `F-13`, head-to-head `F-11`, 3D circuit.

## Build

```bash
./gradlew assembleDebug     # needs JDK 17
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Priorities

1. **Make it run.** Fix compile errors, install it, look at it on a real screen.
2. Check `CarbonNumber` renders — it's the most novel code and the most likely broken.
3. Verify carbon rib at 3× density. It may need to go coarser than 6px on real hardware.
4. Port the remaining screens from `f1-tab-complete.html`.
5. Only then: Tasks. Start local-only (phase 1) — the schema *will* change once it's used for a
   week, and building the CRDT engine against a schema that's about to change is wasted work.

## The thing to be careful with

When Tasks lands, `:feature:tasks:sync` is **the only place a bug silently destroys data you
can't refetch**. Everything else — broken scraper, dead API, laggy UI — is visible and
recoverable. Property-based tests over random op interleavings, asserting N devices converge to
identical state. Never let an error path discard an op log.

# CLAUDE.md: context for this repo

Delta, an F1 tracker. Android, Kotlin, Compose. Sideloaded, single user, no Play Store.
**F1 only.** The Tasks and Valorant tabs are out of scope for good (decided 2026-11-14).
Three tabs: Home, Championship, News. At v1.1.2 the app has Home only, with the bottom bar's
other two buttons inert; V2 builds the rest (`.scratch/f1-v2/spec.md`).

## Read first

| File | What it is |
|---|---|
| `.scratch/f1-v2/spec.md` | The V2 plan: gaps found, page specs, news design, slices. Draft until the owner approves it |
| `design-system.md` | Every visual decision and why. Don't re-derive; don't drift |
| `feature-map.md` | The F1 feature menu with IDs (`F-01`, `C-01`, `N-01`, `W-01`) and status. Use the IDs |
| `architecture.md` | Data sources, cache policy, build order, risks |
| `f1-tab-complete.html` | The visual spec. Open in a browser. Screens not yet ported live here |
| `SECURITY.md` | Threat model. Read before touching anything network, and before News |
| `floorplan/index.html` | Interactive map of the code (gitignored, rebuild by asking to "update the floorplan") |

## Non-negotiables

**No background execution. Ever.** User constraint, stated flatly. No polling, no WorkManager
periodic, no foreground services, no persistent sockets. The app does nothing when closed.
- `AlarmManager` for *known future times* (race start) is fine: the OS wakes you, you do no work.
- Discovering unknown state ("did something just happen?") requires polling, which is banned.
- News refreshes only when its tab opens, and only if the cache is older than 15 minutes.

**No server of our own.** The app fetches public data on open and keeps a disposable copy.
There is nothing to sync and nothing the user owns that could be lost.

**One database: `feeds.db`.** A cache. Excluded from backup, deleted freely, destructive
migration is fine.

## API facts (verified July and November 2026, do not re-guess these)

**Jolpica** `api.jolpi.ca/ergast/f1/`: free, no auth, 1950 to now, **200 requests per HOUR**.
Historical standings list every entrant, and those who never scored have no `position` field at all
(keep it optional). There is no constructors' table before 1958.
Load-bearing and volunteer-run, so cache like it might vanish. Two quirks: `limit` is capped at
100 (page with `offset`), and **standings queries require a season** (a driver's career cannot
come from one call). `current` works as a season alias.

**OpenF1 goes COMPLETELY DARK during live sessions.** Not degraded: locked, historical included:
```
{"detail":"Live F1 session in progress. Global API access (including past sessions)
 is restricted to authenticated users until the session ends."}
```
Never put anything on the race-Sunday path behind OpenF1. This is why `teamColour()` and
`headshotKey()` in `Repo.kt` are **hardcoded maps, not fetches**. Don't "improve" them into API calls.

**MultiViewer** `api.multiviewer.app/api/v1/circuits/{key}/{year}`: free, no auth, real geometry.
Measured 2026-11-14: 21 of the 23 calendar circuits return a 2026 outline (not Madring, not
Sepang). Spa is key `7`. Length is about 9,930 units per km (within 1.3% of official on six checks).
Laps are NOT derivable from length: the 305 km rule matched only 16 of 21 circuits, so laps come
from Jolpica's past winners (`circuits/{id}/results/1`).

**F1 CDN headshots** `media.formula1.com/content/dam/fom-website/drivers/{key}.png`. Transparent
PNG. The path is **not derivable** from `driverId` (Antonelli is `K/ANDANT01_Kimi_Antonelli/andant01`).

**RSS feeds** differ in image location (thumbnail, enclosure, several `media:content` widths,
or an `<img>` inside `content:encoded`), date format and HTML in snippets. The measured
differences and the chosen source set are in the spec. Feeds are untrusted input.

**Live F1 timing** is free via SignalR at `livetiming.formula1.com`. Headers are case-sensitive
(`User-Agent: BestHTTP`). Foreground-only, so it would not violate the no-background rule. See
`architecture.md` section 3.

**Car art exists, and is F1's, not ours.** The team page hero loads each team's transparent 2026 side-on car from `media.formula1.com/image/upload/c_lfill,w_1200/q_auto/v1740000001/common/f1/2026/{folder}/2026{folder}carright.webp` (`Net.car`, folder names pinned in `Repo.carFolder`). Measured 2026-11-14 for all eleven teams. The season is in the path, so each year needs one edit; a 404 just shows no car. An earlier note here said no such art exists. That was wrong. It is copyrighted and carries sponsor logos, so it is hotlinked like the headshots and never stored or bundled.

## Design rules that will get broken by accident

- **No type between 23sp and 38sp.** The gap is the design.
- **Team colour is DATA. Never chrome.** `accentFor()` handles the Ferrari/Audi red collision.
- **Depth on `#050506` comes from light, not shadow.** Inset hairlines. A drop shadow on
  near-black is invisible, which has already caused two "it's missing" bugs.
- **Never `mix-blend-mode` on anything with alpha.** It blends the whole box. Use SVG filters.
- Canvas is `#050506` not `#000` (shadows need headroom). Light canvas `#F8F8F5`, cards white.
- Screenshots: always keep both `screenshots/light/` and `screenshots/dark/` current.

## Current state (V2 in progress, 2026-11-14)

S0 to S4 of the V2 spec and the race reminder shipped as v1.2.0. S5 (News with a Reddit tab), S7 (polish pass) and S8 (team hero with F1's 2026 car, racing-line icon) shipped as v1.3.0 (2026-11-14). S0: three tabs (Home, Championship, News) each with its
own back stack, season read from Jolpica's `current` alias, a season-complete Home. News is a
placeholder until S5. S2: the Championship tab is the real page, with a Drivers and Constructors
toggle, a season selector back to 1950 (past seasons fetched once, kept forever) and a champion
marker taken from final standings. S3: Calendar rows open a race result (past), Race Weekend (next) or a Circuit page (later), and
the circuit page shows the 3D track, derived length, laps, turns and every past winner.

S1: every circuit with a MultiViewer outline draws (21 of 23), Race Weekend shows derived length, real
lap counts and real session times in local time for any race, and the Eau Rouge highlight is Spa-only.
Madring and Sepang have no outline and show a plain message.

Still broken, all tracked in the spec with evidence:
- A retired driver or team in an old Championship table does not open a page (pages are built from the current season).

Decisions: Lead and wire is the News layout, light-mode grey contrast is left as it is, and no
release goes out between slices (News is a placeholder).

## Build

```bash
./gradlew assembleDirectDebug     # needs JDK 17 (the `direct` flavor is the GitHub APK; `play` is the Play build)
adb install -r app/build/outputs/apk/direct/debug/app-direct-debug.apk
```

Release: `./release.sh vX.Y.Z` (refuses to run without `keystore.properties`, verifies the signer).

## How work is run here

Plan in conversation, write the spec to `.scratch/<slug>/spec.md`, get it approved, then use the
workflows in `~/projects/workflow` (see its README). They suit pure logic that unit tests can
prove, such as the news normaliser. They cannot look at a screen, so UI slices are built inline
with emulator screenshots in both themes.

## The thing to be careful with

Feed and API text is untrusted. Strip to plain text, only open `https` links, cap sizes (see
`SECURITY.md`, "What to do when News lands"). A broken scraper or dead API is visible and
recoverable; a hostile feed item is the one new way to hurt the user.

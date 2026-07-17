# Personal Tracker — Architecture Design

**Target:** Android (phone) + desktop companion. Local-first. No cloud server you operate.
**Scope v1:** Three tabs — Tasks, Valorant, F1. Workout deferred.
**Date:** July 2026

---

## 0. The core insight: two data regimes

The app looks like one thing but is architecturally two things bolted together. Getting this
distinction right up front prevents a lot of confused code later.

| | **Tab 1 — Tasks** | **Tabs 2 & 3 — Valorant, F1** |
|---|---|---|
| Who owns the data | You | Someone else |
| Source of truth | The devices | Remote API |
| Write direction | Read/write | Read-only |
| Sync model | Peer-to-peer CRDT merge | Fetch + cache (disposable) |
| Offline behaviour | Fully functional | Serves stale cache, degrades gracefully |
| If you lose it | Catastrophe | Refetch it |
| Conflict resolution | Real problem, needs design | Doesn't exist — server always wins |

**Consequence:** never put these in the same storage abstraction. Tab 1 data is sacred and
syncs. Tabs 2/3 data is a cache you should be willing to `rm -rf` at any moment. Two Room
databases, or at minimum two clearly separated schemas with different backup rules
(`android:fullBackupContent` should include tasks, exclude the feed cache).

This also resolves the apparent contradiction in your brief ("no cloud" vs. "live esports
scores"). There's no contradiction — you're not hosting *your* data anywhere. You're reading
*public* data. Those are different claims.

---

## 1. Stack

| Layer | Choice | Why |
|---|---|---|
| Language / UI | Kotlin + Jetpack Compose | Only sane 2026 default. Opus is strong here. |
| Local DB | Room (2 instances: `tasks.db`, `feeds.db`) | SQLite, mature, Flow support |
| Networking | Ktor client or Retrofit + kotlinx.serialization | Either fine. Ktor if you want the WebSocket client too. |
| Background work | WorkManager | Feed refresh, op-log compaction |
| Long-lived socket | Foreground Service | Twitch EventSub (see §4.2 — this is the one real cost) |
| Prefs | DataStore | |
| Dictation | sherpa-onnx (AAR) + Parakeet TDT 0.6b v3 INT8 | Same model family as Handy |
| Task sync transport | **Syncthing-Fork** (separate app) | Do not build this yourself |
| Desktop companion | Compose Multiplatform *or* just a CLI | See §3.4 — deliberately deferred |

**Module structure:**

```
:app                    — nav host, tab scaffold, theme
:core:db                — Room, entities, DAOs
:core:net               — http client, common interceptors, rate limiting
:core:design            — Compose theme, shared components
:feature:tasks          — UI + repo
:feature:tasks:sync     — op log, HLC, merge engine   ← the interesting one
:feature:tasks:dictate  — sherpa-onnx wrapper
:feature:valorant       — UI + repo + vlrggapi client
:feature:twitch         — EventSub service + Helix client
:feature:f1             — UI + repo + jolpica/openf1 clients
:feature:workout        — (later)
```

Keep `:feature:tasks:sync` free of Android dependencies if you can. It's pure logic, it's the
part most likely to have bugs, and you want to unit-test it hard on the JVM without an emulator.

---

## 2. Tab 1 — Tasks

### 2.1 The sync design (the one genuinely clever bit)

Naive approach: put a `tasks.db` in a Syncthing folder. **This will corrupt.** Two devices
writing one SQLite file, Syncthing shipping whole-file deltas, no awareness of transactions.
Don't.

**Correct approach — single-writer append-only op logs.**

```
<SyncthingFolder>/tracker/
  ops/
    phone-a1b2.jsonl        ← ONLY the phone ever writes this file
    desktop-c3d4.jsonl      ← ONLY the desktop ever writes this file
  snapshots/
    phone-a1b2.snapshot.json
    desktop-c3d4.snapshot.json
```

The trick: **each device writes exclusively to its own file, and only ever appends.** Syncthing
therefore never sees a concurrent modification to the same file — it never has to resolve a
conflict, because one never occurs. It degrades to a dumb file courier, which is exactly what
you want. No `.sync-conflict-2026...` files. Ever.

Each device *reads* every file (including the others') and folds the union of all ops into its
own local `tasks.db`. The DB is a **derived projection**, not the source of truth. Delete it and
it rebuilds from the logs.

**Op format** (one JSON object per line, newline-delimited):

```json
{"op":"set","entity":"task","id":"t_7f3a","field":"title","value":"Book dentist","hlc":"2026-07-17T10:04:22.104Z:0003:phone-a1b2"}
{"op":"set","entity":"task","id":"t_7f3a","field":"done","value":true,"hlc":"2026-07-17T10:31:02.887Z:0000:phone-a1b2"}
{"op":"del","entity":"task","id":"t_7f3a","hlc":"2026-07-18T08:00:00.000Z:0001:desktop-c3d4"}
```

**Merge rule: last-write-wins, per field.** Not per record — per field. This means if you edit
the title on your phone and tick `done` on the desktop while offline, both survive. Per-record
LWW would silently discard one. This costs you nothing extra and removes an entire class of
"why did my edit vanish" bugs.

**On clocks.** LWW needs a comparable timestamp. Wall-clock alone is wrong when device clocks
skew (and phones/desktops *do* skew). Use a **Hybrid Logical Clock**: `(wallMillis, counter,
deviceId)`, compared in that order. ~40 lines of code:

```kotlin
data class Hlc(val wall: Long, val counter: Int, val node: String) : Comparable<Hlc> {
    override fun compareTo(other: Hlc) = compareValuesBy(this, other,
        { it.wall }, { it.counter }, { it.node })   // node breaks ties deterministically
}

class HlcClock(private val node: String) {
    private var last = Hlc(0, 0, node)
    @Synchronized fun now(): Hlc {
        val phys = System.currentTimeMillis()
        last = if (phys > last.wall) Hlc(phys, 0, node)
               else Hlc(last.wall, last.counter + 1, node)
        return last
    }
    @Synchronized fun observe(remote: Hlc) {   // call for every op read from a peer
        val phys = System.currentTimeMillis()
        last = when {
            phys > last.wall && phys > remote.wall -> Hlc(phys, 0, node)
            remote.wall > last.wall -> Hlc(remote.wall, remote.counter + 1, node)
            remote.wall == last.wall -> Hlc(last.wall, maxOf(last.counter, remote.counter) + 1, node)
            else -> Hlc(last.wall, last.counter + 1, node)
        }
    }
}
```

Because `deviceId` is the final tiebreaker, **every device independently computes the same
winner**. Convergence without coordination. That's the whole game.

**Deletes are tombstones**, never line removals — removing a line would break append-only and
resurrect the task on the next merge from a peer that never saw the delete.

**Compaction.** Logs grow forever. Once a log passes ~5k lines, write
`snapshots/<deviceId>.snapshot.json` (the folded state as of HLC *H*), then truncate that
device's *own* log to ops after *H*. Only ever compact your own file — never touch a peer's.
A peer that's been offline for months just replays from its own snapshot plus whatever it has.
Do this in WorkManager, on charge + idle.

**Atomic writes.** Write to `<file>.tmp`, `fsync`, then `rename()`. Syncthing must never observe
a half-written line. On Android also mark the folder `.stfolder`-adjacent files appropriately
and let Syncthing's watcher pick up the rename.

**Why this is worth the effort:** it's ~400 lines total and it makes the "I went out, both
devices drifted, I came home" case a non-event. That's your actual daily workflow.

### 2.2 Data model

```kotlin
@Entity data class Task(
    @PrimaryKey val id: String,       // "t_" + ULID  (sortable, collision-free, no coordination)
    val title: String,
    val notes: String? = null,
    val done: Boolean = false,
    val dueAt: Long? = null,
    val project: String? = null,
    val createdAt: Long,
    val deleted: Boolean = false,     // tombstone
)
```

Use **ULIDs, not autoincrement ints.** Two devices offline both creating "task 47" is exactly
the collision you cannot recover from. ULIDs are generated independently and never collide.

### 2.3 Dictation

Embed `sherpa-onnx` and run **Parakeet TDT 0.6b v3 INT8 ONNX** on-device — the same model
Handy uses on CPU, so accuracy should land in the same ballpark.

Practical notes that will bite you otherwise:

- **Do not bundle the model in the APK.** INT8 at 0.6B params is several hundred MB. Download
  on first run to `filesDir`, show progress, verify checksum. (You're sideloading so Play's
  size limits don't apply — but a 700MB APK is still miserable to iterate on.)
- **Flow:** hold-to-record FAB → capture 16kHz mono PCM → release → transcribe → drop text
  into the title field, editable before commit. Mirror Handy's press/release ergonomics; they're
  good.
- **VAD:** Silero VAD (ships with sherpa-onnx) to trim silence. Meaningfully improves both speed
  and accuracy.
- **Threading:** inference off the main thread, obviously. Expect a cold-start model load of a
  few seconds — keep the session warm while the app is foregrounded.
- **First-run UX:** the model download is your worst onboarding moment. Make the app fully usable
  (typed input) before it completes.

**Stretch goal — system-wide dictation.** Wrap the same engine in an `InputMethodService` and
you get a Parakeet-quality voice IME usable in *every* app, which is strictly better than what
FUTO offers today (Whisper-based). This is genuinely valuable and genuinely more work — it's a
separate battle with IME lifecycle. Ship the in-app version first, then decide.

### 2.4 Desktop side — deliberately deferred

The op-log format is plain JSONL. That means your v1 "desktop app" can honestly be a **60-line
Python script** that folds the logs and prints/edits tasks. Ugly, works, costs you a day.

Compose Multiplatform for a real desktop UI is the eventual answer but it is *not* a v1 problem.
The format is the contract; any client that can append valid JSONL is a first-class peer. Don't
let desktop UI work block the phone app.

---

## 3. Tab 2 — Valorant + Twitch

### 3.1 Match / team / player data

Source: [**vlrggapi**](https://github.com/axsddlr/vlrggapi) — unofficial REST wrapper over VLR.gg.

Gives you: upcoming matches, live matches, results with scorelines, per-map player stats
(rating, ACS, K/D/A, ADR, KAST, HS%, agent), team pages, event/tournament listings, world +
regional rankings, head-to-head history, map veto.

That covers your ask — "teams, their matches, what happened, the scoreline and player overview" —
essentially completely.

**Screens:**
1. **Followed teams** — your list, next match + countdown, last result
2. **Match detail** — per-map scoreline, full player stat table, veto order
3. **Schedule** — upcoming across VCT + regionals
4. **Standings** — event brackets / rankings

**The risk you must accept:** it's a scraper. When VLR.gg changes their HTML, it breaks — not
gracefully, but with parse exceptions. Mitigations:
- Run your own instance (it's self-hostable) rather than depending on someone's public deploy
- Treat *every* field as nullable; never let a parse failure crash the tab
- Cache aggressively — a stale scoreline beats an empty screen
- Pin the version; upstream fixes land after VLR changes, not before

Riot has no public esports API. This is the only game in town, and the tradeoff is real.

### 3.2 Twitch — zero background execution

**Hard rule: no background work. No EventSub socket, no WorkManager poll, no foreground
service.** The app does nothing when it isn't open. This is a constraint, and it turns out to be
a cheap one.

**The whole design:** one Helix call when the tab opens.

```
GET https://api.twitch.tv/helix/streams
    ?user_login=creator1&user_login=creator2&...   (up to 100 per request)
```

Helix accepts **up to 100 `user_login` params in a single request**, and returns an entry only
for channels that are *currently live*. So your entire followed list resolves in **one request**,
in ~200ms, on tab open. Anyone in the response is live; anyone absent isn't. There is no polling
loop to build because there is no loop — it's a single fetch tied to a UI event.

Cache the result for ~60s so tab-switching doesn't re-hit the API. Pull-to-refresh for a manual
re-check. That's the feature, complete.

**What you give up:** you learn someone is live when you open the app, not before. Given the
constraint, that's not a compromise — it's the only correct design. Anything else is a
background process wearing a costume.

**The door you left open.** You said push is acceptable *if it comes from an external service*.
That's a real option and it's cheaper than it sounds:

```
Twitch EventSub (webhook transport) → Cloudflare Worker (~50 lines, free tier) → FCM → phone
```

The Worker is stateless — it holds no data of yours, it just receives `stream.online` and
forwards a notification. Your phone does **zero** background work; FCM delivery is handled by
the system push channel that's already running regardless. This is genuinely the clean answer if
you later want real-time, and it doesn't violate your no-server-for-my-data principle, because
the relay stores nothing.

**Don't build it now.** Ship the on-open fetch, live with it for a month, see if you actually
care. The Helix client is identical either way.

**Auth:** register a Client ID at dev.twitch.tv. Use **Device Code Flow** — no client secret in a
distributed app, no redirect URI juggling on mobile. `/streams` needs only an app-level token;
`stream.online` (if you ever go the Worker route) requires no special scope either. Store tokens
in `EncryptedSharedPreferences`.

### 3.3 Wiring the two together

Nice touch, cheap to build: on a Valorant match detail screen, show a **live "watch" badge** if
any followed creator is currently streaming that match's game/category. Cross-referencing data
you're already fetching for both halves of the tab. Small effort, disproportionate payoff — this
is the kind of thing that makes a personal app feel like *yours* rather than a worse version of
an existing product.

---

## 4. Tab 3 — F1 (full menu, you cut)

> ### ⚠ Verified against the live APIs, July 2026 — four things changed
>
> 1. **OpenF1's free tier goes COMPLETELY DARK during live sessions.** Not degraded — locked,
>    historical data included, behind the €9.90 key. Confirmed by hitting it during Belgian GP
>    practice: `{"detail":"Live F1 session in progress. Global API access (including past
>    sessions) is restricted to authenticated users until the session ends."}`
>    **Your F1 tab would break every race weekend — exactly when you'd open it.** This promotes
>    Jolpica from "preferred" to **load-bearing**, and makes permanent caching non-optional.
>    Anything you need on a Sunday must not come from OpenF1.
> 2. **MultiViewer's circuit API is real, free, unauthenticated, and excellent.**
>    `api.multiviewer.app/api/v1/circuits/{key}/{year}` → 1005 points, 19 corners, rotation, all
>    22 tracks. This is what OpenF1 credits. Use it directly; skip the middleman.
> 3. **`team_colour` and `headshot_url` must be hardcoded, not fetched.** They only exist in
>    OpenF1 (see #1). 11 teams and 22 drivers change once a year — a constant is more reliable
>    than a request. The headshot path isn't derivable anyway: Antonelli lives under `K/` but is
>    coded `ANDANT01`.
> 4. **Car liveries are a dead end.** F1 publishes no 2026 car art; 2025 exists at 465×139 with
>    no alpha (a list thumbnail). The constructor hero uses both drivers instead.

### 4.1 Sources — including the one I missed

There are **three** sources, not two. The third changes the picture completely.

| | **Jolpica-F1** | **OpenF1 (free)** | **OpenF1 (paid)** | **F1 SignalR (direct)** |
|---|---|---|---|---|
| Cost | Free | Free | **€9.90/mo** | **Free** |
| Auth | None | None | Yes | **None** |
| Coverage | **1950 → now** | 2023 → now | 2023 → now | Live sessions only |
| Rate limit | 200 req/**hour** | 3 req/s, 30/min | 6 req/s, 60/min | n/a (stream) |
| Live | No | No | Yes | **Yes** |
| Depth | Results | Telemetry | Telemetry + live | **Everything** |

**The thing I got wrong.** I told you live timing costs €9.90/mo. That's true *of OpenF1*. But
OpenF1 is not the source — it's a reseller. Its own `/team_radio` responses hand out
`recording_url`s pointing at **`https://livetiming.formula1.com/static/...`**, which gives the
game away: **OpenF1 ingests F1's own live timing feed, which is public and unauthenticated.**

You can connect to that feed directly. It's what [FastF1](https://github.com/theOehrly/Fast-F1)
reverse-engineered and what [undercut-f1](https://github.com/JustAman62/undercut-f1) (a live
timing TUI) connects to today.

**The protocol** — classic ASP.NET SignalR 1.5, two steps:

```
1) GET https://livetiming.formula1.com/signalr/negotiate
       ?connectionData=[{"name":"Streaming"}]&clientProtocol=1.5
   → { ConnectionToken, ... }  + a Set-Cookie you must keep

2) wss://livetiming.formula1.com/signalr/connect
       ?clientProtocol=1.5&transport=webSockets
       &connectionToken=<urlencoded>&connectionData=[{"name":"Streaming"}]

   Headers (CASE-SENSITIVE — wrong case returns 500, missing returns 400):
     User-Agent: BestHTTP
     Accept-Encoding: gzip,identity
     Cookie: <from step 1>

3) Send: {"H":"Streaming","M":"Subscribe","A":[["TimingData","Heartbeat"]],"I":1}
   ("A" really is an array of arrays of string.)
```

**All 16 subscribable streams:**

```
Heartbeat            TimingData           TimingStats          TimingAppData
CarData.z            Position.z           DriverList           LapCount
SessionInfo          SessionData          TrackStatus          ExtrapolatedClock
WeatherData          RaceControlMessages  TopThree             RcmSeries
```

`.z` suffix = zlib-deflated + base64. Inflate before parsing. `CarData.z` and `Position.z` are
the big ones (full telemetry + track position for all 20 cars).

**Why this fits your no-background rule perfectly.** You only connect **while the app is
foregrounded during a session** — you're watching the race, the app is open, the socket lives,
you close it, the socket dies. That's not background work; that's a foreground feature. No
service, no notification, no Doze fight. The constraint that killed Twitch push doesn't apply
here at all.

**Caveats, honestly:** this is unofficial and undocumented. There's no discovery mechanism, the
stream names above are community-reverse-engineered, and F1 can change it without warning (they
have before). It's what every F1 tool in the ecosystem does, and it's for personal use — but it's
not a contract. Budget for it breaking occasionally, and keep Jolpica as the source of truth for
anything that matters.

**Recommendation:** **Jolpica** for all history/results (free, 1950+, immutable, cacheable) +
**direct SignalR** for live sessions (free, foreground-only). **OpenF1 free tier** only if you
want its convenience for 2023+ post-session analysis. **Skip the €9.90 entirely** — you'd be
paying for a proxy to a feed you can reach yourself.

**Two constraints to design around:**

1. **Jolpica's 200 req/hour is tight** and unauthenticated. Bulk-fetch a season on first open,
   cache permanently. Historical F1 data is immutable — a 2019 result will never change. Only the
   current season needs refreshing. Done right: ~20 requests a week.
2. **Jolpica is volunteer-run with a *break-even* 2026 fundraising goal.** Cache like it might
   vanish, because it might. Consider bundling a historical snapshot in the APK.

**OpenF1 detail worth stealing** even if you don't use it much: it supports comparison operators
inline in the URL — `?speed>=315`, `?lap_number<=3`, `?interval>0&interval<0.005`. Repeating a
param ORs it. That's a big payload reduction on telemetry endpoints. Also `?csv=true` on anything.

### 4.2 The menu

Effort is rough dev-days assuming Opus drafts and you integrate.

| # | Feature | Source | Coverage | Effort | Verdict |
|---|---|---|---|---|---|
| 1 | Next race countdown + calendar | Jolpica | 1950+ | 0.5d | **Free win.** Do it. |
| 2 | Driver + constructor standings | Jolpica | 1950+ | 0.5d | **Core.** Do it. |
| 3 | Race results browser (any race, any year) | Jolpica | 1950+ | 1d | **Core.** Cache forever. |
| 4 | Qualifying results (Q1/Q2/Q3 splits) | Jolpica | 1994+ | 0.5d | Cheap once #3 exists |
| 5 | Sprint results | Jolpica | 2021+ | 0.25d | Trivial add-on to #3 |
| 6 | Pit stop table | Jolpica | 2011+ | 0.5d | Cheap |
| 7 | DNF / finishing status | Jolpica | 1950+ | 0.25d | Cheap |
| 8 | Lap-by-lap position chart | Jolpica `/laps` | 1996+ | 2d | **Best effort:payoff ratio.** The classic spaghetti chart. Data's free, all the work is rendering. |
| 9 | Teammate head-to-head (quali + race) | Jolpica, computed | 1950+ | 1.5d | **Underrated.** Pure local SQL over cached results. Zero extra API cost. Endlessly interesting. |
| 10 | Driver/constructor career stats | Jolpica, computed | 1950+ | 1.5d | Local computation. Free. |
| 11 | Weather per session | OpenF1 `/weather` | 2023+ | 0.25d | Trivial. Updated every minute. |
| 12 | Race control feed (flags, SC, incidents) | OpenF1 `/race_control` | 2023+ | 0.5d | Nice race-replay texture |
| 13 | Tyre stint visualisation | OpenF1 `/stints` | 2023+ | 1d | Strategy nerd catnip. Compound + `tyre_age_at_start`. |
| 14 | Speed-trap / sector / mini-sector times | OpenF1 `/laps` | 2023+ | 1d | Mini-sector colours. **Not available during races** — practice/quali only. |
| 15 | ~~Team radio playback~~ | OpenF1 `/team_radio` | **effectively dead** | 1d | **Cut — I was wrong to recommend this.** See below. |
| 16 | **Overtake log** | OpenF1 `/overtakes` | 2023+ | 0.5d | *Missed this one.* Who passed whom, on which lap, incl. pit-stop swaps. Cheap and genuinely fun. |
| 17 | **Starting grid vs. finish delta** | OpenF1 `/starting_grid` + `/session_result` | 2023+ | 0.5d | *Also missed.* "Who gained most places" — trivial once both are cached. |
| 18 | Live championship swing during a race | OpenF1 `/championship_drivers` | 2023+ | 0.5d | Beta endpoint. Points as they'd stand right now. |
| 19 | Telemetry traces (speed/throttle/brake/RPM/gear/DRS) | OpenF1 `/car_data` | 2023+ | 3d | 3.7Hz. Use URL operators to trim payloads. |
| 20 | Track map + car positions (replay) | OpenF1 `/location` | 2023+ | 4d | X/Y/Z, arbitrary origin. Circuit geometry via `circuit_info_url` (MultiViewer) — **you don't have to draw the tracks yourself.** |
| 21 | **Live timing during sessions** | **F1 SignalR direct** | live | 4–5d | **Free, not €9.90.** Foreground-only. The real prize. |

**On #15, team radio — a correction.** I called this "delightful, genuinely unusual" last round.
OpenF1's own docs say, verbatim:

> "Team radio is not released by F1 for every session. **Coverage has decreased significantly
> starting in 2026, with most events providing no radio data at all.** This is a limitation on
> F1's side and outside our control."

So I recommended a feature that mostly returns empty arrays this season. Cut it.

**Two deprecations to code around:** `country_code` on `/drivers` and `pit_duration` on `/pit`
are both removed **at the end of the 2026 season**. Use `lane_duration` instead of `pit_duration`
now. Also `stop_duration` (stationary time, as opposed to total pit lane time) only exists from
the **2024 US GP onwards**.

### 4.3 What I'd actually keep

**Tier 1 — ~3 days, 80% of your actual usage:** 1, 2, 3, 4, 5, 7 — *Jolpica only*
**Tier 2 — ~5 days, the stuff that makes it yours:** 8, 9, 10 — *Jolpica only*, all local
computation over cached data, zero marginal API cost, fully offline
**Tier 3 — ~2.5 days of cheap wins:** 11, 12, 13, 16, 17, 18 — *adds OpenF1 free tier*
**Tier 4 — the prize:** 21 (live timing, free via SignalR)
**Heavy, do when bored:** 19, 20
**Cut:** 15 (dead data)

Tiers 1+2 need **only Jolpica** — one source, no subscription, offline, back to 1950. That's a
clean, complete, genuinely useful F1 app in ~8 days. Tier 3 is where you take on OpenF1 as a
second dependency; Tier 4 is where you take on an undocumented reverse-engineered protocol. Cross
those boundaries deliberately, not by drift.

**If you only build one thing beyond Tier 1:** #9 (teammate head-to-head). It's pure SQL over
data you've already cached, it costs one extra API call ever, and it answers the question F1 fans
actually argue about.

---

## 5. Cross-cutting

**Rate limiting.** Jolpica's 200/hr will bite you during development — hot-reloading a screen
that fetches on composition will burn through it in minutes. Build a token-bucket interceptor in
`:core:net` **on day one**, not after you get your first 429. Persist the bucket across process
death.

**Cache policy per source:**

| Data | TTL |
|---|---|
| F1 historical (season < current) | **Forever.** Immutable. |
| F1 current season standings | 24h, + force-refresh Sunday evening |
| Valorant results | 6h |
| Valorant live match | 60s while foregrounded, never in background |
| Twitch live status | 60s, fetched on tab open only — **no background work** |
| F1 live timing | Not cached — foreground stream, dies with the screen |

**Offline.** Every tab must render from cache with an "as of <time>" marker. Never a spinner that
never resolves. This isn't polish — it's the point of the app.

**Secrets.** Twitch Client ID in `local.properties` → BuildConfig, never committed. It's a
personal sideloaded app so the threat model is mild, but don't push tokens to a public repo out
of habit.

---

## 6. Build order

Each phase should end with something you actually use. Don't build breadth-first.

| Phase | Deliverable | ~Time |
|---|---|---|
| **0** | Compose scaffold, 3 empty tabs, nav, theme, Room ×2, rate-limit interceptor | 2d |
| **1** | **Tasks, local only.** No sync, no voice. Typed CRUD. **Use it for a week.** | 3d |
| **2** | Op log + HLC + merge engine, JVM unit tests only, no UI | 3d |
| **3** | Wire sync to Syncthing folder. Test: airplane-mode both devices, edit both, reconnect, verify convergence | 2d |
| **4** | 60-line Python desktop folder/editor | 1d |
| **5** | **F1 Tier 1** (Jolpica: schedule, standings, results) | 3d |
| **6** | **Valorant** (vlrggapi: teams, matches, player stats) | 4d |
| **7** | **Twitch** — Device Code auth + single on-open `/streams` fetch | 1d |
| **8** | **Dictation** — sherpa-onnx + Parakeet, model download, hold-to-record | 4d |
| **9** | F1 Tier 2 (lap charts, H2H, career stats) | 5d |
| **10** | F1 Tier 3 (weather, race control, stints, overtakes, grid delta) | 2.5d |
| **11** | **F1 live timing** — SignalR client, foreground-only, during a real session | 5d |
| **12+** | Voice IME / F1 telemetry + track map / Cloudflare Worker push / workout tab | — |

**Why Tasks first and sync second:** you'll redesign the task schema once you actually live with
it for a week. Building the merge engine against a schema you're about to change is wasted work.
Phase 1 is deliberately throwaway-tolerant.

**Why dictation is late (phase 8):** it's the feature you're most excited about and the least
load-bearing. If it slips, you have a working tracker. If sync slips, you have nothing. Build the
scary structural thing while you have energy for it; save the fun self-contained thing for when
you don't.

---

## 7. Risks

| Risk | Severity | Response |
|---|---|---|
| **vlrggapi breaks when VLR.gg changes HTML** | High / likely | Self-host, null-tolerant parsing, aggressive cache. Accept it *will* happen. |
| **Jolpica shuts down** (volunteer-run, break-even goal) | **High impact / possible** | Now load-bearing (OpenF1 is unusable on race weekends), so this went from annoying to serious. Cache permanently; bundle a historical snapshot. Data is immutable — an offline copy stays correct forever. |
| **OpenF1 locks out every race weekend** | **Confirmed, not a risk — a fact** | Never put anything on the Sunday path behind OpenF1. Hardcode `team_colour` / `headshot_url`. Treat OpenF1 as a Mon–Thu enrichment source only. |
| **Syncthing on Android** — official app discontinued | Med | Use **Syncthing-Fork** (F-Droid, maintained). Verify before building on it. |
| **Merge engine has a subtle bug → silent data loss** | **High / this is your real risk** | Property-based tests on the JVM. Generate random op interleavings, assert convergence. Never delete op logs on error. |
| **F1 changes the SignalR feed** | Med / has happened before | Undocumented, reverse-engineered, no contract. Live tab degrades to "unavailable"; Jolpica keeps everything else working. Watch FastF1's commits — they patch fast. |
| SignalR headers are case-sensitive | Low / annoying | `User-Agent: BestHTTP` exactly. Wrong case → 500, missing → 400. OkHttp may normalise headers; verify on the wire. |
| Parakeet accuracy below Handy | Low | Same model. If it disappoints, it's audio pipeline (sample rate, VAD), not the model. |
| Model download bloats first-run UX | Med | App fully usable without it. |
| **Scope creep** | **High** | You already deferred workout. Hold that line — and note F1 alone now has 21 candidate features. |

---

## 8. The one thing to get right

If you take nothing else from this: **the op-log merge engine is the only part where a bug
silently destroys data you can't get back.** Everything else — a broken scraper, a dead F1 API, a
laggy notification — is visible, recoverable, and refetchable.

So: test that engine harder than feels proportionate. Property-based tests, random op
interleavings, assert that N devices applying the same op set in any order reach byte-identical
state. And **never** have an error path that discards an op log. Append-only means append-only,
even when your parser is confused.

---

## Sources

- [Handy](https://github.com/cjpais/Handy) — reference dictation UX, Parakeet V3 on CPU
- [sherpa-onnx prebuilt Android APKs](https://k2-fsa.github.io/sherpa/onnx/android/prebuilt-apk.html) — Parakeet TDT 0.6b v3 on arm64
- [local-whisper](https://github.com/gabrimatic/local-whisper) — working Android + sherpa-onnx + Parakeet reference
- [vlrggapi](https://github.com/axsddlr/vlrggapi) — unofficial VLR.gg REST API
- [Twitch EventSub / Helix docs](https://dev.twitch.tv/docs/api/)
- [Jolpica-F1](https://github.com/jolpica/jolpica-f1) — Ergast successor ([rate limits](https://github.com/jolpica/jolpica-f1/blob/main/docs/rate_limits.md))
- [OpenF1](https://openf1.org/) + [full endpoint docs](https://openf1.org/docs) — telemetry; free tier historical-only, live behind €9.90/mo
- [**Connecting to the F1 SignalR endpoint**](https://dweik.xyz/post/f1-signalr-endpoint/) — the protocol writeup: negotiate, headers, Subscribe, full stream list
- [FastF1](https://github.com/theOehrly/Fast-F1) — reverse-engineered the SignalR stream; [livetiming client source](https://github.com/theOehrly/Fast-F1/blob/main/fastf1/livetiming/client.py)
- [undercut-f1](https://github.com/JustAman62/undercut-f1) — live timing TUI reading the feed directly; working reference implementation

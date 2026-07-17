# Personal Tracker — Complete Feature Map

Companion to `personal-tracker-architecture.md`. Every feature, with an ID, so we can talk about
them precisely when we design the frontend.

**Legend**

- **Effort** — rough dev-days, assuming Opus drafts and you integrate/debug
- **Prio** — `V1` ship it · `V1.5` soon after · `LATER` when bored · `CUT` don't
- **Dep** — blocking feature IDs

---

## 0. The notification rule (applies to all three tabs)

Your "no background execution" constraint has a sharper edge than it first appears, and getting
it right unlocks more than you'd expect. There are two different things people call
"notifications":

| | **Scheduled local notification** | **Discovering unknown state change** |
|---|---|---|
| Example | "Race starts in 30 min", "Task due at 5pm" | "Is xyz live on Twitch right now?" |
| Known in advance? | **Yes** | **No** |
| Mechanism | `AlarmManager` — OS fires it | Requires polling or push |
| Background work | **Zero.** App is asleep. | Continuous |
| Allowed under your rule | ✅ **Yes** | ❌ No |

**This is not a loophole.** `AlarmManager` genuinely does no work — you hand the OS a timestamp,
your process exits, the OS wakes you at that instant. It's the same mechanism as a clock alarm.

**Consequence:** you *can* have race-start reminders, match-start reminders, and task due
alerts — because those times are known when you fetch the schedule. You *cannot* have "creator
went live" — that's unknowable in advance, so it needs polling (banned) or external push
(the Cloudflare Worker door, deferred).

So the rule isn't "no notifications." It's **"no notifications about things you can't predict."**

---

## 1. Tab 1 — Tasks

Source of truth: your devices. Syncs via op-log over Syncthing.

### 1.1 Core CRUD

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| T-01 | Task list view | 0.5d | **V1** | — | The spine. Flat list first. |
| T-02 | Create task (typed) | 0.5d | **V1** | T-01 | |
| T-03 | Task title | 0.1d | **V1** | T-02 | |
| T-04 | Task description / notes | 0.3d | **V1** | T-02 | You asked for this explicitly. Multiline. |
| T-05 | Complete / uncomplete | 0.2d | **V1** | T-01 | Checkbox. Not a delete. |
| T-06 | Edit task | 0.3d | **V1** | T-02 | |
| T-07 | Delete task (tombstone) | 0.3d | **V1** | T-06 | Never removes a log line. |
| T-08 | Task detail screen | 0.5d | **V1** | T-01 | |

### 1.2 Organisation

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| T-10 | Due date | 0.5d | **V1** | T-02 | |
| T-11 | Projects / lists | 1d | **V1.5** | T-01 | Single-level. Resist nesting. |
| T-12 | Tags | 0.5d | LATER | T-01 | Overlaps T-11. Pick one, probably projects. |
| T-13 | Priority (3 levels) | 0.3d | **V1.5** | T-02 | |
| T-14 | Sort (due / created / priority / manual) | 0.5d | **V1.5** | T-01 | Manual sort needs a fractional index — non-trivial under CRDT merge. |
| T-15 | Filter (done / project / overdue) | 0.5d | **V1.5** | T-01 | |
| T-16 | Search | 0.5d | **V1.5** | T-01 | SQLite FTS5. |
| T-17 | Today / Upcoming smart views | 1d | **V1.5** | T-10 | Derived queries, no new storage. |
| T-18 | Subtasks | 2d | LATER | T-01 | Parent ref + merge semantics for orphans. More expensive than it looks. |
| T-19 | Recurring tasks | 2.5d | LATER | T-10 | **Genuinely nasty under CRDT** — two devices can both spawn the next instance. Needs a deterministic instance ID derived from (rule, occurrence date), not a random one. |

### 1.3 Dictation

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| T-20 | sherpa-onnx + Parakeet TDT v3 INT8 engine | 2d | **V1** | — | The Handy-equivalent core. |
| T-21 | Model download on first run + progress + checksum | 1d | **V1** | T-20 | Several hundred MB. Don't bundle. |
| T-22 | Hold-to-record FAB → transcribe → editable field | 1d | **V1** | T-20, T-02 | Mirror Handy's press/release. |
| T-23 | Silero VAD silence trimming | 0.3d | **V1** | T-20 | Ships with sherpa-onnx. Real accuracy gain. |
| T-24 | Dictate into description (not just title) | 0.2d | **V1.5** | T-22, T-04 | |
| T-25 | Warm-session keep-alive while foregrounded | 0.3d | **V1.5** | T-20 | Kills the multi-second cold load. |
| T-26 | Model picker (small/large tradeoff) | 0.5d | LATER | T-21 | |
| T-27 | **System-wide voice IME** | 5d | LATER | T-20 | Parakeet-quality dictation in *every* app. Strictly better than FUTO (Whisper). Separate battle with IME lifecycle. **The most valuable LATER item in this doc.** |

### 1.4 Sync

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| T-30 | HLC clock | 0.5d | **V1** | — | ~40 lines. |
| T-31 | Op log append (JSONL, atomic write+rename) | 1d | **V1** | T-30 | |
| T-32 | Op log fold → Room projection | 1d | **V1** | T-31 | |
| T-33 | Per-field LWW merge | 1d | **V1** | T-32 | |
| T-34 | Tombstone handling | 0.5d | **V1** | T-33 | |
| T-35 | Property-based convergence tests (JVM) | 1d | **V1** | T-33 | **Non-negotiable.** The only silent-data-loss surface in the app. |
| T-36 | Syncthing folder integration + file watching | 1d | **V1** | T-31 | |
| T-37 | Compaction / snapshots | 1d | **V1.5** | T-32 | Own log only, never a peer's. |
| T-38 | Sync status indicator ("last merged 2m ago") | 0.5d | **V1.5** | T-36 | Small UI, big trust payoff. |
| T-39 | Desktop CLI folder/editor (Python) | 1d | **V1** | T-31 | Ugly, works, 1 day. |
| T-40 | Desktop GUI (Compose Multiplatform) | 8d+ | LATER | T-39 | Deliberately deferred. Format is the contract. |

### 1.5 Notifications

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| T-45 | Due-date reminder via `AlarmManager` | 0.5d | **V1.5** | T-10 | ✅ Known time. Zero background. Reschedule on merge. |

**Tasks total: V1 ≈ 12d · V1.5 ≈ 6d**

---

## 2. Tab 2 — Valorant + Twitch

Source: `vlrggapi` (unofficial VLR.gg scraper) + Twitch Helix. Read-only cache.

### 2.1 Valorant core

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| V-01 | Followed teams list (your teams, next match, last result) | 1d | **V1** | — | The tab's home. |
| V-02 | Team picker / follow management | 0.5d | **V1** | V-01 | |
| V-03 | Upcoming match schedule | 0.5d | **V1** | — | |
| V-04 | Results feed (recent, with scorelines) | 0.5d | **V1** | — | |
| V-05 | Match detail — per-map scoreline | 1d | **V1** | V-04 | You asked for this. |
| V-06 | Match detail — full player stat table | 1d | **V1** | V-05 | rating, ACS, K/D/A, ADR, KAST, HS%, agent |
| V-07 | Team detail — roster + recent form | 1d | **V1.5** | V-01 | |
| V-08 | Map veto order | 0.3d | **V1.5** | V-05 | Cheap once V-05 exists. |
| V-09 | World + regional rankings | 0.5d | **V1.5** | — | |
| V-10 | Event / tournament browser | 1d | **V1.5** | — | |
| V-11 | Bracket view | 2d | LATER | V-10 | Rendering brackets is fiddlier than it looks. |
| V-12 | Player detail / career stats | 1.5d | LATER | V-06 | |
| V-13 | Head-to-head history between two teams | 1d | LATER | V-04 | |
| V-14 | Agent / map composition stats | 1.5d | LATER | V-06 | |
| V-15 | Live match indicator + score (foreground only) | 0.5d | **V1.5** | V-03 | 60s refresh while the tab is open. Never in background. |

### 2.2 Twitch

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| V-20 | Device Code Flow auth | 0.5d | **V1** | — | No client secret in the APK. |
| V-21 | Followed creators list + management | 0.5d | **V1** | V-20 | |
| V-22 | **Live-now fetch on tab open** (single Helix call, ≤100 logins) | 0.5d | **V1** | V-21 | One request = whole list. ~200ms. That's the entire feature. |
| V-23 | Pull-to-refresh | 0.1d | **V1** | V-22 | |
| V-24 | Deep-link out to Twitch app | 0.2d | **V1** | V-22 | |
| V-25 | Stream metadata (title, game, viewers, thumb) | 0.3d | **V1.5** | V-22 | Free — it's in the same response. |
| V-26 | ~~Live push notification~~ | — | **CUT** | — | Needs polling or external push. Banned by your rule. |
| V-27 | Cloudflare Worker → FCM push relay | 2d | LATER | V-20 | The door you left open. Stateless, free tier, holds none of your data. Build only if V-22 genuinely annoys you. |

### 2.3 The cross-over

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| V-30 | "Watch live" badge on a match if a followed creator is streaming it | 0.5d | **V1.5** | V-05, V-22 | Cross-references data you already have. **Highest payoff-per-line in the app.** This is what makes it feel like yours. |

### 2.4 Notifications

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| V-35 | Match-start reminder via `AlarmManager` | 0.5d | **V1.5** | V-03 | ✅ Known time from schedule. Zero background. |

**Valorant total: V1 ≈ 6.5d · V1.5 ≈ 5d**

---

## 3. Tab 3 — F1

Source: Jolpica (history, 1950+, free) + OpenF1 (telemetry, 2023+, free tier) + F1 SignalR
(live, free). See architecture doc §4.

### 3.1 Tier 1 — Jolpica only, core

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-01 | Next race countdown + calendar | 0.5d | **V1** | — | Tab home. |
| F-02 | Driver standings | 0.3d | **V1** | — | |
| F-03 | Constructor standings | 0.2d | **V1** | F-02 | |
| F-04 | Race results browser (any race, any year 1950+) | 1d | **V1** | — | Cache forever — immutable. |
| F-05 | Qualifying results (Q1/Q2/Q3 splits) | 0.5d | **V1** | F-04 | 1994+ |
| F-06 | Sprint results | 0.25d | **V1** | F-04 | 2021+ |
| F-07 | DNF / finishing status | 0.25d | **V1** | F-04 | |
| F-08 | Pit stop table | 0.5d | **V1.5** | F-04 | 2011+ |
| F-09 | Season selector / history navigation | 0.5d | **V1** | F-04 | Unlocks the 1950+ archive. |

### 3.2 Tier 2 — Jolpica only, computed locally (zero marginal API cost)

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-10 | Lap-by-lap position chart | 2d | **V1.5** | F-04 | The classic spaghetti chart. Data free, all work is rendering. 1996+ |
| F-11 | **Teammate head-to-head (quali + race)** | 1.5d | **V1.5** | F-04 | Pure SQL over cache. **If you build one thing past Tier 1, this.** |
| F-12 | Driver career stats | 1.5d | **V1.5** | F-04 | |
| F-13 | Constructor history | 1d | LATER | F-04 | |
| F-14 | Circuit detail + past winners | 0.5d | LATER | F-04 | |
| F-15 | Season comparison / records | 1.5d | LATER | F-12 | |

### 3.3 Tier 3 — adds OpenF1 free tier (2023+)

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-20 | Weather per session | 0.25d | LATER | — | Updated every minute. |
| F-21 | Race control feed (flags, SC, incidents) | 0.5d | LATER | — | Good replay texture. |
| F-22 | Tyre stint visualisation | 1d | LATER | — | `compound` + `tyre_age_at_start`. Strategy catnip. |
| F-23 | Overtake log (who passed whom, which lap) | 0.5d | LATER | — | Incl. pit swaps. Cheap and fun. |
| F-24 | Starting grid vs. finish delta | 0.5d | LATER | — | "Who gained most places." |
| F-25 | Speed trap / sector / mini-sector times | 1d | LATER | — | **Not available during races** — practice/quali only. |
| F-26 | Live championship swing during a race | 0.5d | LATER | — | Beta endpoint. |
| F-27 | ~~Team radio playback~~ | 1d | **CUT** | — | 2026 coverage: "most events providing no radio data at all." Dead. |

### 3.4 Tier 4 — live timing (free, via direct SignalR)

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-30 | SignalR client (negotiate → wss → Subscribe) | 2d | LATER | — | Case-sensitive headers. `.z` = zlib+base64. |
| F-31 | Live timing tower (positions, gaps, intervals) | 1.5d | LATER | F-30 | `TimingData` |
| F-32 | Live track status / flags / SC | 0.5d | LATER | F-30 | `TrackStatus`, `RaceControlMessages` |
| F-33 | Live lap counter + session clock | 0.3d | LATER | F-30 | `LapCount`, `ExtrapolatedClock` |
| F-34 | Broadcast-sync delay slider | 0.5d | LATER | F-31 | Feed is ~3s *ahead* of TV. Without this it spoils the race for you. undercut-f1 does exactly this — steal the idea. |
| F-35 | Live telemetry (`CarData.z`) | 1.5d | LATER | F-30 | |
| F-36 | Live track map (`Position.z`) | 2d | LATER | F-30, F-41 | |

### 3.5 Heavy / later

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-40 | Telemetry traces (speed/throttle/brake/RPM/gear/DRS) | 3d | LATER | — | 3.7Hz. Use URL operators to trim payloads. |
| F-41 | Track map + position replay | 4d | LATER | — | Circuit geometry via `circuit_info_url` (MultiViewer) — **you don't draw the tracks yourself.** |

### 3.6 Notifications

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| F-45 | Race/quali start reminder via `AlarmManager` | 0.5d | **V1.5** | F-01 | ✅ Schedule known months ahead. Zero background. |

**F1 total: V1 ≈ 3d · V1.5 ≈ 5.5d · LATER ≈ 22d**

---

## 4. Cross-cutting

| ID | Feature | Effort | Prio | Dep | Notes |
|---|---|---|---|---|---|
| X-01 | Compose scaffold + 3-tab nav + theme | 2d | **V1** | — | |
| X-02 | Room ×2 (`tasks.db` sacred / `feeds.db` disposable) | 0.5d | **V1** | — | Different backup rules. |
| X-03 | Token-bucket rate limiter (persisted across process death) | 1d | **V1** | — | **Day one.** Jolpica's 200/hr will bite during dev. |
| X-04 | Offline "as of <time>" staleness markers | 0.5d | **V1** | — | Never an unresolving spinner. |
| X-05 | Error/empty states per tab | 1d | **V1** | — | Scraper *will* break. Null-tolerant everywhere. |
| X-06 | Settings screen | 1d | **V1.5** | — | Syncthing path, model, follows, API keys. |
| X-07 | Dark theme | 0.5d | **V1.5** | X-01 | |
| X-08 | Home-screen widget (today's tasks / next race) | 2d | LATER | — | Widgets update via OS-scheduled refresh — allowed. |
| X-09 | Workout tab | ? | LATER | — | Deferred by you. Hold the line. |

**Cross-cutting total: V1 ≈ 5d · V1.5 ≈ 1.5d**

---

## 5. Totals

| | V1 | V1.5 | LATER |
|---|---|---|---|
| Tasks | 12d | 6d | ~18d |
| Valorant | 6.5d | 5d | ~8d |
| F1 | 3d | 5.5d | ~22d |
| Cross-cutting | 5d | 1.5d | ~2d+ |
| **Total** | **~26d** | **~18d** | **~50d** |

**~26 dev-days to V1.** At evenings-and-weekends pace, call it 2–3 months. That's a real project,
not a weekend. Worth knowing before you start.

Note the shape: **Tasks is 46% of V1 effort** despite being the least glamorous tab. That's
correct — it's the only tab where you own the data and the only one with a hard correctness
problem. F1 is the cheapest tab in V1 (3 days!) because Jolpica does the work.

---

## 6. Screen inventory (the bridge to frontend)

Derived from the V1 + V1.5 features above. **This is what we're designing.**

### Tasks
1. **Task list** — the home. List + FAB + mic. `T-01,02,05,22`
2. **Task detail / edit** — title, description, due, project, priority. `T-04,06,08,10,13`
3. **Quick-add sheet** — typed or dictated, minimal friction. `T-02,22`
4. *(V1.5)* **Smart views** — Today / Upcoming / project filter. `T-17,15`

### Valorant
5. **Tab home** — followed teams, next match, last result, live-now creators strip. `V-01,22`
6. **Match detail** — per-map scoreline, player stat table, veto. `V-05,06,08`
7. **Schedule** — upcoming matches. `V-03`
8. **Results feed** — recent results. `V-04`
9. *(V1.5)* **Team detail** — roster, form. `V-07`
10. *(V1.5)* **Rankings**. `V-09`

### F1
11. **Tab home** — next race countdown, current standings snapshot. `F-01,02,03`
12. **Race detail** — results, quali, sprint, pits, DNFs. `F-04,05,06,07,08`
13. **Standings** — driver + constructor, season selector. `F-02,03,09`
14. **Season browser** — the 1950+ archive. `F-09`
15. *(V1.5)* **Lap chart**. `F-10`
16. *(V1.5)* **Head-to-head**. `F-11`
17. *(V1.5)* **Driver detail**. `F-12`

### Shared
18. **Settings**. `X-06`
19. **Follow management** (teams + creators). `V-02,21`

**19 screens for V1+V1.5.** Nine are V1.

### Navigation model

```
Bottom nav (3 tabs — 4 when workout lands)
├── Tasks     → list → detail
│                   → quick-add sheet
├── Valorant  → home → match detail
│                   → schedule / results
│                   → team detail (V1.5)
└── F1        → home → race detail
                    → standings → season browser
                    → driver detail (V1.5)
```

Bottom nav is right here: three peer destinations, no hierarchy between them, frequent switching.
Don't overthink it. Each tab keeps its own back stack.

---

## 7. Open questions for frontend design

1. **Does the app have one visual identity, or three?** Tasks wants calm and out-of-the-way.
   Valorant/F1 want dense data and team/constructor colour. A single flat theme will make one of
   them feel wrong. My instinct: one structural system (spacing, type, components), per-tab accent
   colour. Worth deciding deliberately.
2. **How prominent is the mic?** It's your headline feature but it only lives in one tab. FAB on
   the task list, or global?
3. **Dense or airy?** F1 results and Valorant stat tables are inherently tabular. Material 3
   defaults are quite airy — a 20-driver results table at default density needs a lot of
   scrolling.
4. **Landscape?** Lap charts and stat tables genuinely want it. Everything else doesn't.
5. **What's the cold-open default?** Tasks is the utility; the other two are leisure. Probably
   Tasks — but if you open the app 10× on a race Sunday, maybe it's contextual.

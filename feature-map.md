# Delta: F1 feature map

Every feature has an ID so we can refer to it precisely. This replaces the three-tab map
(Tasks, Valorant, F1). Those tabs are out of scope; the old map is in git at tag `v1.1.2`.

Scope for V2 is in `.scratch/f1-v2/spec.md`. This file is the full menu.

**Status**: Built = in the app today. V2 = planned slice S0 to S6. Later = wanted, not scheduled. Cut = decided against.

## 0. The notification rule

The app does no background work. That leaves one kind of notification: a *scheduled local* one,
where the app hands the OS a timestamp it already knows and exits (`AlarmManager`). Race and
qualifying start times are known months ahead, so a reminder is allowed. "Something just
happened" alerts need polling or a push server, and are banned.

## 1. Tabs and screens

| Tab | Screen | Status |
|---|---|---|
| Home | Home | Built |
| Home | Race weekend (next race) | Built, any circuit with an outline |
| Home | Calendar | Built, rows open the result, the weekend or the circuit |
| Home | Race result | Built |
| Home | Circuit | Built |
| Championship | World Championship (Drivers and Constructors toggle, season selector) | Built |
| Championship | Driver | Built, with career, history and teammate |
| Championship | Team | Built, with points by round, career and history |
| News | News: Press and Reddit | Built |

## 2. Core results (Jolpica only)

| ID | Feature | Status | Notes |
|---|---|---|---|
| F-01 | Next race countdown and calendar | Built | Season-complete state added in V2 S0 |
| F-02 | Driver standings | Built | On the Championship page |
| F-03 | Constructor standings | Built | On the Championship page; none exist before 1958 |
| F-04 | Race result for any round | Built | Current season only, from the daily season download |
| F-05 | Qualifying results with Q1, Q2, Q3 | Later | The head-to-head (F-11) needs the data anyway |
| F-06 | Sprint results | Later | 2021 onward |
| F-07 | DNF and finishing status | Built | Counted on the driver page |
| F-08 | Pit stop table | Later | 2011 onward |
| F-09 | Season selector, back to 1950 | Built | Past seasons fetched per season, cached forever |

## 3. Computed locally from cached results

| ID | Feature | Status | Notes |
|---|---|---|---|
| F-10 | Lap-by-lap position chart | Later | Best effort to payoff of the heavy features |
| F-11 | Teammate head-to-head | Built | Race finish and starting slot (qualifying results are not used). Zero requests |
| F-12 | Driver career stats and season history | Built | 5 requests for totals, 1 per season for history, loaded on a tap |
| F-13 | Team history | Built | Wins, podiums, season history. No poles before 1994 data |
| F-14 | Circuit page with past winners | Built | One request per circuit, kept forever |
| F-15 | Season comparison and records | Later | |

## 4. Championship and News (new)

| ID | Feature | Status | Notes |
|---|---|---|---|
| C-01 | Championship page, Drivers and Constructors toggle | Built | Rows open driver and team pages; past drivers open only if they race in the current season |
| C-02 | "World Champion" marker on the leader row | Built | From the final standings, never computed. Shown for every past season and for the current one once the last round is in |
| N-01 | News list from free RSS feeds, one normalised look | Built | Eight sources, lead and wire layout, fetch on tab open, 15 minute cache |
| N-02 | Source filter chips | Later | |
| N-03 | Duplicate story collapsing | Built | Title similarity, keep the one with an image |
| N-04 | Reddit tab: r/formula1, r/formuladank or r/f1discussion, one followed at a time | Built | Hot order from the Atom feed, no scores or comments (not in the feed). Posts open in Reddit |

## 5. Race weekend and tracks

| ID | Feature | Status | Notes |
|---|---|---|---|
| W-01 | 3D track for every circuit | Built | 21 of 23 circuits have an outline, measured |
| W-02 | Real session times in local time | Built | From the calendar response |
| W-03 | Track length, laps and turns | Built | Length derived from the outline (about 1% off), laps from past winners, turns from the corner count |
| W-04 | Track top speed and elevation | Cut | No free source found, owner dropped it |

## 6. OpenF1 and live timing

| ID | Feature | Status | Notes |
|---|---|---|---|
| F-20 | Weather per session | Later | OpenF1 is locked during live sessions, so Monday to Thursday only |
| F-21 | Race control feed | Later | |
| F-22 | Tyre stint chart | Later | |
| F-23 | Overtake log | Later | |
| F-24 | Grid vs finish delta | Later | |
| F-25 | Speed trap and sector times | Later | Not available during races |
| F-26 | Live championship swing | Later | Beta endpoint |
| F-27 | Team radio | Cut | OpenF1 reports almost no radio data in 2026 |
| F-30 to F-36 | Live timing over SignalR, foreground only | Later | See `architecture.md` section 3 |
| F-40, F-41 | Telemetry traces, track position replay | Later | |
| F-45 | Race and qualifying start reminders | Built (race only) | One notification an hour before lights out; the bell on Home turns it on or off. Qualifying is not covered |

## 7. Cross-cutting

| ID | Feature | Status |
|---|---|---|
| X-01 | Compose scaffold, navigation, theme | Built, three tabs since V2 S0 |
| X-02 | Room cache (`feeds.db`) | Built |
| X-03 | Persisted token-bucket rate limiter | Built |
| X-04 | Offline "as of" markers, never an endless spinner | Partial, V2 |
| X-05 | Error and empty states per screen | Partial, V2 |
| X-06 | Dark and light theme from the system | Built |
| X-07 | Pull to refresh | Built | Home, Championship and News, after the Compose BOM bump to 2024.09.03 |
| X-08 | Home-screen widget (next race) | Later |

## 8. Effort

V2 totals about 12 dev-days: S0 1, S1 1.5, S2 2, S3 2, S4 3, S5 2.5, S6 0.2. Estimates, not measurements.

# App screenshots

Captured on a Pixel 6 emulator (Android 14, API 34), live Jolpica data. All files
were re-shot on 2026-11-14 after V2 slices S0 to S7 (three-tab bar, Round 21 Las Vegas is next, Weekend works for any
circuit with an outline).
The app follows the system theme, so there are two full sets:

- **`dark/`** — dark canvas (`#050506`)
- **`light/`** — light canvas (`#F5F5F3`)

Same filenames in each:

| File | Screen |
|---|---|
| `01-home.png` | Home — next race, countdown, top-5 standings, constructors |
| `02-championship.png` | Championship — Drivers table, current season |
| `03-driver-antonelli.png` | Driver — Antonelli (Mercedes teal glow, carbon #12) |
| `04-driver-hamilton.png` | Driver — Hamilton (Ferrari red glow, carbon #44) |
| `05-weekend.png` | Race weekend — 3D circuit hero (Las Vegas) + length, laps, turns |
| `06-weekend-schedule.png` | Race weekend — session schedule in the phone's local time, race accented |
| `07-calendar.png` | Calendar (F-09) — 22 rounds, past dimmed, next accented, future neutral |
| `08-constructor.png` | Constructor (F-13): stripe-band hero with the team's 2026 car, driver chips, stats, team facts |
| `09-news.png` | News tab, Press |
| `10-championship-constructors.png` | Championship — Constructors toggle |
| `11-championship-1988.png` | Championship — a past season (1988) with the champion marker |
| `12-race-result.png` | Race result: Belgian GP classification |
| `13-circuit.png` | Circuit page: Spa with Eau Rouge, facts and past winners |
| `18-home-footer.png` | Home scrolled to the bottom: the support note (direct build only) and the unofficial line |
| `19-learn-index.png` | Learn F1: the lessons as a staggered starting grid, with a fun fact of the day |
| `20-learn-points.png` | Learn F1 lesson page template (Points): season stamp, text and tables on hairlines |
| `17-season-sheet.png` | Championship: the season picker as a bottom sheet |
| `16-news-reddit.png` | News: the Reddit tab with the subreddit picker |
| `14-driver-depth.png` | Driver page, lower half: career, teammate head-to-head, recent results, seasons |
| `15-team-depth.png` | Team page, lower half: points by round, career, seasons |

Regenerate both sets by toggling `adb shell cmd uimode night yes|no` and re-shooting each flow.
Stat card and results chart derive from `/results`; circuit geometry from MultiViewer.

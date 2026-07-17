# App screenshots

Captured on a Pixel 6 emulator (Android 14, API 34), live Jolpica data (Round 10 — Belgian GP).
The app follows the system theme, so there are two full sets:

- **`dark/`** — dark canvas (`#050506`)
- **`light/`** — light canvas (`#F5F5F3`)

Same filenames in each:

| File | Screen |
|---|---|
| `01-home.png` | Home — next race, countdown, top-5 standings, constructors |
| `02-standings.png` | Standings — full championship grid |
| `03-driver-antonelli.png` | Driver — Antonelli (Mercedes teal glow, carbon #12) |
| `04-driver-hamilton.png` | Driver — Hamilton (Ferrari red glow, carbon #44) |
| `05-weekend.png` | Race weekend — 3D circuit hero (Spa) + circuit stats |
| `06-weekend-schedule.png` | Race weekend — session schedule, Sunday race accented |

Regenerate both sets by toggling `adb shell cmd uimode night yes|no` and re-shooting each flow.
Stat card and results chart derive from `/results`; circuit geometry from MultiViewer.

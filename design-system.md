# Personal Tracker — Design System

The synthesis we arrived at, and *why*. Read the reasoning, not just the values — most of these
were wrong at least once, and the reasons are what stop them regressing.

**Lineage:** structure from [Box Box Club](https://boxbox.club) (poster type, hero bleed, stat
rows). Palette from [Nothing OS](https://nothing.tech) (four greys, one red, no gradients).
Roundness and shadows are ours — neither reference does them.

Live reference: `f1-tab-complete.html`. Code: `app/src/main/java/dev/anpo/f1/ui/Theme.kt`.

---

## 1. The one-line thesis

> **Flat chrome, loud data.**

Chrome is monochrome, quiet, and identical everywhere. All colour on screen comes from *data* —
team liveries, agent art, charts, photos. The UI never has an opinion about colour.

The first attempt read this as **flat = plain** and produced iOS system grey and 14px list rows.
That's wrong. Flat means *no bevels*; it does not mean *no atmosphere*. The canvas is black and
lit; the type is poster-scale. Restraint lives in the palette, not the presence.

---

## 2. Canvas

| Token | Dark | Light |
|---|---|---|
| canvas | `#050506` | `#F5F5F3` |
| card | `#131316` | `#FFFFFF` |
| chip / raised | `#1E1E23` | `#F0F0EE` |
| nav / pill | `#16161A` | `#FFFFFF` |

**Dark canvas is `#050506`, not `#000`.** Deliberate: shadows need something to fall onto and
pure black has no room below it. Five units of headroom is enough to make elevation land.

**Light canvas is `#F5F5F3`, not white — and cards are pure white.** If you invert that (white
canvas, grey cards) the cards *recede* instead of lifting and every shadow dies. **In light mode
the card must be the brightest thing on screen.**

### Ambient glow

Two radial gradients per screen. Warm, low alpha, bleeding from the edges.

```
dark:  radial(rgba(215,25,33,0.30) 0%, rgba(215,25,33,0.09) 46%, transparent 72%)
light: radial(rgba(215,25,33,0.13) 0%, rgba(215,25,33,0.045) 46%, transparent 72%)
```

Light mode is the fragile part: at `0.13` it reads as warmth; at `0.20` it's a pink smear. This
is also the least-Nothing thing in the system — their brand doc explicitly rejects gradients. We
keep it because it's the atmosphere Box Box gets its character from. Know that you're trading.

**On hero screens the glow comes from the data**, not the theme — `team_colour` lights the
driver page, so every driver gets their own atmosphere free.

---

## 3. Type ladder

| Role | Size | Weight | Tracking |
|---|---|---|---|
| Hero stat | 48 | 800 | −2 |
| Display / countdown | 40 | 800 | −1.7 |
| Name | 38 | 800 | −1.5 |
| Section | 23 | 800 | −0.7 |
| Stat value | 19 | 700 | — |
| Body | 15 | 500 | — |
| Meta | 12 | 400 | — |
| Label | 10 | 400 | +0.5 |

**The gap is the design. Nothing lives between 23 and 38.** Box Box has almost no mid-sized
type, and that jump from 48 to 10 with nothing between is the entire effect. Adding a 28px
"subheading" would quietly destroy it.

**Titles wrap on purpose.** "Belgian / Grand Prix" across two lines filling the column is the
poster. A short name gets **bigger**, never narrower. "United States GP" isn't long by accident.

**The ladder only lands with air around it.** First attempt used the right sizes at 340px wide,
compressed the vertical to fit, and it read as cramped — the sizes were right, the space wasn't.
Section headers get **34px above them**. That gap does as much work as the type size.

---

## 4. Roundness

| Element | Radius |
|---|---|
| Phone | 44 |
| List / content card | 28 |
| Tile | 22 |
| Date chip, nav chip, avatar | circle |
| Pill / tab bar | 999 |

Roundness only works **systemically**. One square element and the whole thing looks accidental —
which is why nav icons became round chips and date numbers became circles rather than bare text.

---

## 5. Shadows — and the OLED black problem

**Shadows do not exist on `#000`.** A drop shadow needs something darker to fall onto. This bit
us twice: the nav bar "had no shadow" and the driver number "had no separation" — both were
present in code and invisible in fact.

Dark mode gets depth from **light**, not shadow:

```css
/* card */
box-shadow: 0 8px 24px rgba(0,0,0,0.8),        /* barely visible; still helps */
            inset 0 0.5px 0 rgba(255,255,255,0.06);  /* THIS is what lifts it */
```

The inset top hairline is doing the work. Light mode is the reverse — shadows become the primary
cue and drop **~10×** in strength:

```css
box-shadow: 0 8px 22px rgba(0,0,0,0.07), 0 2px 6px rgba(0,0,0,0.04);
/* no inset highlight — dark-mode-only trick */
```

**Floating elements on black need blur, not shadow.** The nav bar reads as floating because of
`backdrop-filter: blur(26px)` over a translucent fill plus a `0 0 0 .5px rgba(255,255,255,.10)`
hairline ring — content visibly blurs *under* it. No shadow value would have worked.

---

## 6. Palette

Four greys from Nothing, one red, nothing else.

| Token | Dark | Light | Use |
|---|---|---|---|
| txt | `#FFFFFF` | `#0A0A0A` | Values, names |
| qua | `#C5C5C5` | `#9A9A94` | Active icons |
| ter | `#A7A7A7` | `#A7A7A7` | Passive icons |
| sec | `#848484` | `#848484` | Labels, meta |
| **accent** | `#D71921` | `#D71921` | The only accent |

**Only two of the five invert.** `#848484` and `#A7A7A7` are identical in both modes — mid-greys
are perceptually stable against both ends. That's why Nothing chose that ladder, and it means
half your tokens are mode-independent. The accent doesn't move either.

---

## 7. Colour is data — and the accent-yields rule

Team colour **never appears in chrome**. Not in headers, not in buttons, not in the tab bar. It
lives in stripes, charts, glows, duotones, and the driver's surname.

**The problem:** Ferrari is `#ED1131`. Audi is `#F50537`. Nothing Red is `#D71921`. **Three
reds.** In a standings list, an Audi stripe eight pixels from a red "View all" reads as a
rendering *bug*, not a distinction.

**The rule:** when a page's team colour is within ~30° hue of the accent, chrome falls back to
monochrome and lets the team own the colour.

```kotlin
fun accentFor(teamColour: Color, p: F1Palette): Color {
    val d = hueDistance(hueOf(teamColour), hueOf(p.accent))
    return if (d < 30f) p.txt else p.accent
}
```

Two screens of twenty-two, resolved automatically from data you already have. On the driver page
the composition solved it for free — the two reds ended up 300px apart, so they read as separate
things.

**Unresolved:** two channels (white + red) won't encode eight variables. Fine for Spa's one DRS
zone; the Miami tyre-stress screen used five neon colours. That screen either breaks the palette
or finds another encoding — shape, weight, position. Solve it deliberately; don't quietly reach
for cyan.

---

## 8. Hero composition

Three treatments were considered; **C (duotone poster)** won, then evolved.

**Layout:** ghost number behind → subject centred, bleeding off the bottom → **POS left, PTS
right at 40px flanking the figure at eye level** → name centred at the bottom.

The centred figure *creates* the side space; use it. Stats were 11px grey in a corner first —
they're now the second thing you read after the face.

**Photos run in original colour.** The duotone was solving "22 mismatched photos", but F1's
headshots come from one studio setup and don't need it. Natural colour makes the driver a person
rather than a graphic. Team identity is carried entirely by the surroundings.

**Bleeding requires a mask.** The source is cropped at the chest; bleeding it just shows the
crop. Fade it:

```css
mask-image: linear-gradient(to bottom, #000 62%, rgba(0,0,0,.72) 82%, transparent 100%);
```

**Alpha rule. This will bite again on any art with transparency:**
> `mix-blend-mode` blends the whole box. SVG filters blend the pixels.

A `::after` overlay with `mix-blend-mode: color` painted a solid slab over the transparent areas
of the PNG. Anything with an alpha channel needs `feColorMatrix` + `feComponentTransfer`, which
pass the alpha row through untouched.

---

## 9. The hero number

Four effects, composable independently:

| Effect | How |
|---|---|
| **Border** | `-webkit-text-stroke: 3px #43434F` |
| **Carbon fill** | `background-clip: text` + `color: transparent` |
| **Engrave** | duplicate glyph, +3px down, `rgba(255,255,255,.06)` |
| **Glow** | `filter: drop-shadow(...)` ×2 — neutral tight, team-colour wide |

**Engrave** is the trick: a sliver of light peeking below each edge reads as light catching the
lower lip of a recess. It also glints through the texture gaps, which sells it better than a
solid fill would. In light mode it inverts to `rgba(0,0,0,.07)` — same +3px offset, since the
light source stays top-left in both modes.

**Glow via `filter: drop-shadow`, not `text-shadow`** — drop-shadow follows the composited alpha,
so it wraps the stroke *and* the texture rather than a phantom solid glyph.

**Separation comes from the figure, not the backdrop.** The number and figure were both dark at
similar luminance, so the edge between them was invisible — that's mush, not layering. Two fixes
work: a die-cut halo (canvas-coloured stroke around the subject) or giving the number a
different *material* (hollow + lit vs. matte + filled). We took the second. The general
principle: halo/differentiate the subject once, and anything can sit behind it. Tuning the
backdrop per screen is a forever job.

### Carbon fibre

Real 2×2 twill — **one dominant diagonal rib**, low contrast, near-black. Early versions read as
a checkerboard because the rib alternated; they were also far too contrasty. Ship the seamless
PNG, not a gradient stack.

**Rib: 6px.** This is the realistic scale, and it's what was chosen — the number is a *backdrop*,
not a feature, so subtle is correct. Note that a 1x mockup makes it look mushier than it will be;
at 3× a 6px rib gets 18 physical pixels. **Verify on device.** If you ever clip a real texture
into type, scale it ~2× — realistic scale inside a glyph is the trap that looks correct and reads
as grey.

---

## 10. Density

Compact in data views (stat tables, results, standings, news rows). Colour carries
information, so rows need less spacing to stay readable. A 22-driver classification fits one
screen at 12px rows with 7px padding.

---

## 11. Navigation

Floating pill, inset 20px, **30px off the bottom**, fully round, backdrop-blurred. **Icons only,
27dp**, no labels. Active = accent, inactive = `sec`. A tab bar welded to the bottom edge would
be the one square element undoing all the roundness.

Three tabs (Home, Championship, News) with distinct glyphs is fine icon-only. Icon-only
navigation degrades fast past three or four destinations, so a fourth tab means adding labels.

---

## 12. Open questions

1. **Does it survive a stat table?** Untested. The Championship tables and the race
   classification are the first dense tables, with team colour and position competing in every
   row. Those are the screens that could break the system.
2. **Does it survive a text-heavy list?** Untested. News rows carry a headline, a snippet and
   a source on every line, from eight publishers with different image shapes.
3. **The five-colour data problem** (§7).
4. **Light mode duotone/glow** is acceptable, not great. Less headroom than dark, always will be.

---

## 13. Don'ts

- Don't add type between 23 and 38.
- Don't put team colour in chrome.
- Don't use a drop shadow for depth on dark. Use light.
- Don't use `mix-blend-mode` on anything with alpha.
- Don't make one element square.
- Don't add a second accent. If it collides, chrome yields — the data never does.
- Don't tune the backdrop to avoid the subject. Differentiate the subject.

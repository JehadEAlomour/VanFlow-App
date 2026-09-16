# FlowVan Design System — porting guide for Compose apps

A field-sales UI system for Android / Compose Multiplatform, extracted from the
FlowVan salesman app. Everything here is copy-paste ready: the tokens are the
real values in `core/design-system`, and the numbers in the scale sections were
measured from what the 60-odd screens actually use, not aspirational.

Written for someone starting a **different** app who wants this look and these
guarantees. Read the Constraints section first — several decisions here are
deliberate refusals, and copying the palette without them gives you a system
that looks right and fails outdoors.

---

## 1. Constraints this system was built around

These are the reasons the rest of the document looks the way it does. If your
app does not share them, change the tokens rather than fighting the rules.

**Light only. There is no dark theme, and adding one is a real decision.**
The users sell in the morning, outdoors, in direct sun, where a dark interface
cannot be read at all. Because the palette never has to compromise between two
grounds, it commits to what daylight needs: high contrast, solid fills, and
borders heavy enough to survive glare. `AppTheme` takes no `darkTheme`
parameter — not defaulted to `false`, *absent* — because a parameter invites a
call site to pass `true`, and the print screens and every contrast figure
assume a light ground.

**Every text colour clears 4.5:1 on white.** There is deliberately no
pale-grey-on-white in the set. The previous `TextLow` (`#A8B3C6`) measured
2.1:1 and was unreadable in the field; that single measurement is why the text
ramp is only three steps.

**Arabic-first, RTL-first.** The interface font is bundled, not requested from
the system, and the layout is written so mirroring is free.

**Colour carries meaning, never decoration.** A rep reads the colour before the
number. Green is cash, amber is credit, red is a return or a debt. Reaching for
a semantic colour to liven up a screen breaks the only fast-read channel the UI
has.

**Flat.** Solid fills, a visible 1px border, a small radius. No gradients and
no elevation except where something genuinely floats.

---

## 2. What to copy (and what to ignore)

The repo contains two colour layers. Only one is real:

| Layer | Status | Use it? |
|---|---|---|
| `Fv` (`components/FvColors.kt`) | **1091 usages** across screens | **Yes — this is the system** |
| `ColorTokens` / `ExtendedColors` / `LightColorScheme` | 7 usages of `MaterialTheme.colorScheme`, **0** of `AppTheme.extendedColors` | No — vestigial |

The Material token layer is still wired into `AppTheme` so Material components
(ripples, text fields, dialogs) render sensibly, and that is all it does. Port
`Fv`; keep a minimal `lightColorScheme` only to give Material something
reasonable to fall back to.

The same applies to `ExtendedTypography` — a full 40-style Material scale that
screens do not call. They set `fontSize`/`fontWeight` directly. Port the font
family and the size ramp in §4; port the 40-style object only if you intend to
use it, which FlowVan does not.

---

## 3. Colour

Copy this file verbatim, rename the object, adjust hues if your brand differs —
but keep the *structure*: three grounds, three surfaces, three text steps, six
semantics, one border.

```kotlin
import androidx.compose.ui.graphics.Color

/**
 * Light only. Every text colour clears 4.5:1 against [Surface].
 */
object Fv {
    // ── Grounds ──────────────────────────────────────────────
    val BgDeepest   = Color(0xFFF2F5FA)   // page behind everything
    val Bg          = Color(0xFFEDF1F8)

    // ── Surfaces ─────────────────────────────────────────────
    val Surface     = Color(0xFFFFFFFF)   // cards, sheets, rows
    val SurfaceHigh = Color(0xFFF2F5FA)
    val SurfaceTop  = Color(0xFFE6EBF4)   // pressed / inert fills

    // ── Text ─────────────────────────────────────────────────
    val TextHigh = Color(0xFF0B1626)      // 16.9:1 — values, titles
    val TextMid  = Color(0xFF4A5A73)      //  7.4:1 — labels
    val TextLow  = Color(0xFF6E7C93)      //  4.6:1 — the lightest permitted

    // ── Semantic (meaning, not decoration) ───────────────────
    val Blue   = Color(0xFF1B5FD9)        // accent, primary action
    val Green  = Color(0xFF0B8F58)        // cash, success
    val Amber  = Color(0xFF9A5B00)        // credit, warning, over limit
    val Red    = Color(0xFFC42F2F)        // return, debt, danger
    val Teal   = Color(0xFF0B7E74)        // stock, quantities
    val Purple = Color(0xFF5B4AA8)        // legacy call sites only

    /** Visible at arm's length in daylight, unlike a true hairline. */
    val Border = Color(0xFFD9E1EE)
}
```

**Rules.**

- `TextLow` is a floor, not a starting point. Nothing lighter goes on `Surface`.
- Semantic colours are for state, not for variety. If a screen looks dull, fix
  the hierarchy with weight and size, not with hue.
- Tinted chip backgrounds use the semantic colour at ~20% alpha over the
  surface (`Color(0x33F04F4F)` style), with the solid semantic colour as the
  foreground. Keep the pair; a tinted background with grey text loses the
  signal.
- `Border` is load-bearing. At 1dp on white it is what separates a card from
  the page in sunlight, so it is a mid-grey rather than the near-white hairline
  most systems use.

---

## 4. Typography

**Almarai (المراعي)**, bundled with the app — three weights: Regular, Bold,
ExtraBold. No Light.

Two reasons it is bundled rather than requested: the platform Arabic default is
a different face on every handset in the field, so a layout proved on one phone
reflows on the next; and these devices are frequently offline, where a
downloaded font is not an option. No Light weight because at 11sp read at arm's
length in sunlight a 300 weight disappears — so it is not shipped, rather than
discouraged.

```kotlin
@Composable
fun almaraiFamily(): FontFamily = FontFamily(
    Font(Res.font.almarai_regular,   FontWeight.Normal),
    Font(Res.font.almarai_bold,      FontWeight.Bold),
    Font(Res.font.almarai_extrabold, FontWeight.ExtraBold),
)
```

**The size ramp actually in use** (measured across the app, most-used first):

| sp | Used for | Weight typically |
|---|---|---|
| 11 | chip and stat labels, captions | Medium / SemiBold |
| 12 | secondary rows, metadata | Medium |
| 13 | body, list rows, buttons in bars | Normal / Bold |
| 14 | list titles | Bold |
| 15 | full-width button labels | Bold |
| 16–18 | screen titles, card headings | ExtraBold |
| 22 | hero stat values | ExtraBold |

Two notes. 10sp and 9sp exist in the codebase for dense metadata under a title —
use sparingly and never for anything a decision depends on. And headline weight
is carried by **ExtraBold**, not size: a 18sp ExtraBold title beats a 24sp
Regular one on a phone in sun.

---

## 5. Shape and spacing

**Radius** (measured usage, most common first):

| dp | Used for |
|---|---|
| 8 | the default — chips, buttons, notices |
| 10 | list cards |
| 12 | larger cards, sheets |
| 6 | dense inline pills |
| 14–18 | hero panels |
| 4 | inputs, tight controls |

If you want one number: **8dp**. `AppShapes` maps Material's `small/medium/large`
to 4/8/12dp.

**Spacing.** `8.dp` is the default gap, `10.dp` for list separation, `12–16.dp`
for section breaks. Screen padding is `14.dp` horizontal. Vertical rhythm inside
a card is `padding(vertical = 14.dp)` for a tap target, `3.dp` for a chip.

**Touch targets.** Buttons are `padding(vertical = 14.dp)` on a full-width
surface — roughly 48dp tall. These are used with one thumb, often in a moving
van; nothing actionable goes below 44dp.

---

## 6. Component recipes

The building blocks that matter. Each is a flat surface + border + 8dp radius;
that consistency is most of the system.

### Primary action

Full-width, filled for primary, outlined otherwise. The detail worth copying is
the **busy** state: a busy button keeps its live colours even though it is not
clickable, because the disabled fill is near-white and a white spinner on it is
invisible — the user reads a save in flight as a button that did nothing.

```kotlin
@Composable
fun FvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    busy: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = when {
            !enabled && !busy -> Fv.SurfaceTop
            primary -> Fv.Blue
            else -> Fv.Surface
        },
        border = if (primary) null else BorderStroke(1.dp, Fv.Border),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(
                    color = if (primary) Color.White else Fv.Blue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    label,
                    color = when {
                        !enabled -> Fv.TextLow
                        primary -> Color.White
                        else -> Fv.Blue
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
```

### Status chip

Tinted background, solid semantic foreground, 11sp, 8dp radius,
`padding(horizontal = 8.dp, vertical = 3.dp)`.

```kotlin
@Composable
fun StatusChip(text: String, tone: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .background(tone.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = tone,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
```

### Stat hero

A gradient panel with evenly spaced stat columns separated by hairline
dividers — the one place gradients are allowed, because it is the screen's
single focal point.

```kotlin
Box(
    Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF185FA5), Color(0xFF0C447C))))
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(value = "…", label = "…")
        Box(Modifier.width(0.5.dp).height(48.dp).background(Color.White.copy(alpha = 0.2f)))
        StatColumn(value = "…", label = "…")
    }
}
```

Value at 22sp ExtraBold with `maxLines = 1` and `TextOverflow.Ellipsis`; label
at 11sp Medium, `Color.White.copy(alpha = 0.6f)`. **Three stats fit
comfortably; four is the practical ceiling** on a 360dp phone before values
start truncating.

### Screen states

The single highest-value rule in this system: **"nothing here" and "the request
failed" must never look the same.** In FlowVan, eleven report screens had each
grown their own idea of empty, and on most of them a rep could not tell an
empty month from a failed load. Provide three distinct states — `Loading`,
`Empty(message)`, `Error(message, onRetry)` — from one shared kit, so the
distinction is made once and inherited everywhere.

### List row

`Surface` fill, 1dp `Fv.Border`, 10dp radius, title 14sp Bold `TextHigh`,
metadata 10–11sp `TextLow`, trailing value right-aligned and monospaced-ish via
`fontVariantNumeric`-style tabular alignment where digits stack.

---

## 7. Porting checklist

1. **Create the module.** `core/design-system` as its own Gradle module, with
   `commonMain` if you are multiplatform. Everything below lives in it.
2. **Bundle the font.** Put the three Almarai weights in
   `commonMain/composeResources/font/` and expose `almaraiFamily()`. Substitute
   your own face if the app is not Arabic, but keep it bundled and keep three
   weights.
3. **Copy `Fv`.** Adjust hues to your brand; keep the structure and re-check
   contrast if you do. Anything you put on `Surface` must clear 4.5:1.
4. **Write `AppTheme`** with no `darkTheme` parameter. Provide a minimal
   `lightColorScheme` so Material components behave, plus `AppShapes` at
   4/8/12dp.
5. **Build the kit before the screens.** Button, chip, card, top bar, and the
   three screen states. Screens that predate the kit grow their own versions of
   all five, and unifying them afterwards is the expensive way round.
6. **Set up strings for RTL from day one.** Two resource files
   (`values/`, `values-en/`) with Arabic as the default, and a parity check in
   CI if you can — a key present in one locale and missing in the other is a
   runtime crash, not a missing label.

---

## 8. Things this system deliberately does not have

Worth stating so a porter does not add them back by reflex:

- **No dark theme.** See §1.
- **No elevation/shadows** except where something genuinely floats (dialogs,
  sheets). Cards are separated by border, not shadow — shadows vanish in sun.
- **No gradients** outside the stat hero.
- **No Light font weight.**
- **No pale grey text.**
- **No icon-only actions** for anything destructive or financial; a label
  always accompanies the icon.
- **No animated transitions between list states.** A rep tapping through 40
  customers wants the next screen, not a crossfade.

---

## 9. Where to read the originals

| File | Contains |
|---|---|
| `core/design-system/.../components/FvColors.kt` | the `Fv` palette |
| `core/design-system/.../theme/AlmaraiFont.kt` | the font family |
| `core/design-system/.../theme/AppTheme.kt` | theme entry point |
| `core/design-system/.../theme/AppShapes.kt` | radius scale |
| `core/design-system/.../components/ReportKit.kt` | button, notice, grid tile, screen states |
| `core/design-system/.../components/Chips.kt` | chip and badge patterns |
| `feature/voucher/.../VanStockScreen.kt` | a representative screen: hero, filters, list |

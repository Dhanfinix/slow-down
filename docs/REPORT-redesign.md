# Redesign report: Stitch design applied to :app

## Commands and results

| Command | Result |
|---|---|
| `./gradlew assembleDebug testDebugUnitTest lintDebug` | BUILD SUCCESSFUL (28s, final run) |
| Install on emulator-5554 (Android 15) | Success |
| "Slow me down now" (manual start) | VPN came up (`dumpsys connectivity` shows `VPN:com.slowdown.app`), hero showed 128 kbps |
| "Stop slowing" (hero button) | VPN gone (`dumpsys connectivity` count 0) |

`:throttle` and all behavior code (data, logic, watch) are untouched. Only `ui/`, resources and strings changed. Existing unit tests pass unchanged.

## Per-screen table

| Screen | Matches design? | Deviations and why |
|---|---|---|
| Home | Yes, close | Hero shows the live state (Off / Watching / Slowed). Design only shows the slowed state. "Add 15 min" is left out (no such feature). "Release speed" is the existing Stop button, label kept as "Stop slowing". Manual card hidden while slowed (the hero has the stop button). Avatar, "Daily Target 60m" left out (no feature). "Apply Resistance" kept as "Slow me down now". |
| Stats | Partly | Title, week pill, two bar charts (peak day in clay, dashed average line), privacy note done. Left out because the app has no data for them: "Gentle pacing active" card, Late Night Awareness, per-app Watched Apps list, Offline Sanctuary photo, "Preview zero-scroll milestone". |
| Settings | Yes, close | All sections done: thresholds, deceleration presets, precision slider, scope, quiet hours, privacy. Slider max is 1024 kbps (design shows 1200) to keep the existing limit. Quiet-hours times are 24h with no AM/PM chip. Scope texts are generic, not "Instagram, TikTok, Reddit". Local VPN row has no live status dot. |
| Onboarding permissions | Yes, close | "Simulated friction" demo card and the "4s in / 4s out" preview left out (fake). "0 packets transmitted" replaced by "Runs on this device" (the relay does forward traffic). "Skip to manual mode" finishes onboarding like "Begin". |
| Onboarding intro pages (not designed) | Restyled | Same colors and serif titles, step chip, pill page dots. |
| Choose apps (not designed) | Restyled | Rounded rows, clay search field, serif title. |
| Notifications / app icon | Restyled | New icon: sage green background, ring with a slow wave. Status-bar icon redrawn to match. |

## Theme

- Light scheme: exact values from `DESIGN.md` front matter (primary `#36533C`, surface `#F3FCF1`, tertiary clay).
- Dark scheme: derived from the same hues (Stitch gave light only).
- Dynamic color is off always (no setting added).
- Shapes 4/8/12/16/24 dp. Pill buttons, 48 dp touch targets, 20 dp screen margin.
- Motion: 4s breathing circles in the hero, pulsing status dots, wavy progress. Both stop when animations are off.

## Fonts

Bundled in `app/src/main/res/font/`, so they work offline (variable TTFs from google/fonts, OFL):
- Newsreader (regular and italic): titles, wordmark.
- Plus Jakarta Sans: body and controls.
- JetBrains Mono: speeds, counts, labels.

## Screenshots (`docs/screens/`)

Light: `redesign-home-light-top`, `redesign-home-light-bottom`, `redesign-slowed-light`, `redesign-stats-light`, `redesign-settings-light`, `-2`, `-3`, `redesign-settings-light-scope`, `redesign-apps-light`, `redesign-onboarding-1-light`, `redesign-onboarding-perms-light`.
Dark: `redesign-home-dark-top`, `redesign-home-dark-bottom`, `redesign-stats-dark`, `redesign-settings-dark`, `-2`, `-3`, `redesign-apps-dark`.

## Skipped

- Side-by-side pixel diff tool. I compared the screenshots with the Stitch PNGs by eye.
- Dark screenshots of onboarding and the slowed hero. Light slowed hero was checked.
- Tablet / landscape check.
- Stats, Settings visuals checked with real data from earlier test sessions only (one active day).
- Notification look not re-checked on device (only the small icon changed).
- Optional dynamic-color setting.
- Nothing was committed.

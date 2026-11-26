# Redesign verify (independent)

Emulator-5554 (Android 15, 1344x2992). Compared with `docs/stitch/.../screen.png`. Nothing committed. `:throttle` untouched.

## Mismatches found and fixed (all in `/app` UI)

| # | Mismatch | Where | Fix |
|---|---|---|---|
| 1 | Hero "Stop slowing" text hard to read in dark (dark text on dark green, looks disabled) | Home, slowed, dark | `HomeScreen.kt`: `contentColor = onPrimaryContainer` |
| 2 | Duration "Indefinite" selected pill barely visible in dark | Home, dark | selected pill uses `secondaryContainer`, text `onSecondaryContainer` in dark |
| 3 | "Indefinite" label cut ("Indefinit") at font 1.3 on 720x1280 | Home, duration | `autoSize` 9 to 14sp on the label |
| 4 | Intro page body text cut ("you notice." half hidden) at font 1.3, small screen | Onboarding 1 to 3 | `IntroPage` scrolls (`BoxWithConstraints` + `verticalScroll`) |
| 5 | Last card touches the bottom bar (8 dp) | Home, Stats, Settings | bottom padding 24 dp |

## Checked, matches the design (close)
Home (off, watching, slowed), Stats, Settings (all sections), onboarding permissions page, in light and dark. Fonts (Newsreader titles, Plus Jakarta body, JetBrains Mono numbers), clay accent, sage greens, 24 dp cards, pill buttons all match.
Font 1.3 on 1080x1920: no overflow on Home, Settings. Font 1.3 on 720x1280: Home, Stats, Settings, onboarding OK after fixes.

## Regression (final build)
| Check | Result |
|---|---|
| `./gradlew assembleDebug testDebugUnitTest lintDebug` | BUILD SUCCESSFUL |
| Slow now (3G Patient 384, scope Entire device) | `VPN:com.slowdown.app` present, hero 384 kbps |
| Speed, HTTPS 12 s (`Dl`, method from REPORT-e2e) | 387.4 kbps (first run before fixes: 384.2). Error +0.9% |
| Stop button on Home | VPN gone (count 0) |
| Notification "Stop" action | VPN gone (count 0) |
| `logcat` FATAL EXCEPTION / crash buffer | 0 / 0 |

## Screenshots (`docs/screens/verify-*.png`)
Light: `home-light-watching`, `home-light-bottom`, `home-light-slowed`, `stats-light`, `settings-light-1..3`, `apps-light`, `onb-1..4-light`, `onb-4-light-scrolled`.
Dark: `home-dark-off`, `home-dark-bottom`, `home-dark-slowed` (after fix 1), `stats-dark`, `settings-dark-1..3`, `onb-4-dark`.
Large font / small screen: `big-*`, `small-*`. After stop: `after-stop`, `slowed-final`.
Some dark shots (home-dark-off/bottom, settings-dark) were taken before fix 2 and 5.

## Remaining deviations (by design, no feature behind them)
- No avatar, "Add 15 min", "Daily Target 60m".
- Stats: no "Gentle pacing" card, Late Night card, per-app list, photo card, milestone button.
- Settings: slider max 1024 kbps (design 1200), 24 h time fields without AM/PM, generic scope text, no live dot on VPN row.
- Onboarding: no "Simulated friction" card, no "4s in / 4s out". "Skip to manual mode" shows only when usage access is missing. Page 4 has no Back button (system back works).
- Choose apps: no app icons; floating select-all pill has no label and overlaps the last row.
- Slider track is thicker than the design (M3 expressive wavy handle).

## Notes
- Fresh install leaves notification permission off, so the foreground and Stop notifications do not show until it is allowed (onboarding "Allow" does it). I granted it with `pm grant` for the Stop test.
- Not checked: physical phone, tablet/landscape, apps screen in dark, real doom-scroll session after the redesign, release build.

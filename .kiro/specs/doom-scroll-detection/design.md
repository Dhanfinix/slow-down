# Design: doom-scroll detection

Source of truth: `docs/REPORT-app.md` and `docs/REPORT-e2e.md`.

## Flow
```
UsageStatsManager -> UsageEventsReader -> ForegroundTracker -> WatchEngine.tick
  -> DoomScrollDetector.onTick -> Decision (NONE | START | STOP)
  -> ThrottleCoordinator -> ThrottleController (from :throttle) + Notifier
```
`WatcherService` is a foreground service (`specialUse`, subtype `doom_scroll_watcher`) that calls `WatchEngine.tick` every 15 s.

## Parts
| Part | Job |
|---|---|
| `watch/UsageEventsReader` | Reads events since the last poll |
| `logic/ForegroundTracker` | Applies RESUMED, PAUSED, SCREEN_NON_INTERACTIVE to get the foreground package |
| `logic/WatchEngine` | Adds elapsed time to today's stats, feeds the detector |
| `logic/DoomScrollDetector` | Pure Kotlin state machine. No Android types, so it is easy to test with a virtual clock |
| `logic/ThrottleCoordinator` | The only caller of `ThrottleController`. Handles manual timer, notification Stop, VPN revoked |
| `data/*Repository` | DataStore for settings and stats (no Room: 60 days of rows is small) |
| `watch/BootReceiver`, `ActionReceiver` | Boot restart and notification actions |

## Decisions
- The detector is pure so the main invariant can be tested without a device.
- Only `ThrottleCoordinator` touches the engine. One owner avoids two places starting or stopping the VPN.
- Missed ticks end a session, so a killed service cannot cause a throttle by guessing.
- "Time saved" is time spent slowed. It is an estimate, not measured.
- Auto mode never triggers with no watched apps. Manual mode with no watched apps slows all apps.
- Compose UI: Material 3 Expressive (`material3 1.5.0-alpha08`, because 1.4.0 keeps the expressive APIs internal), type-safe Navigation Compose, ViewModels with StateFlow, dynamic color.

## Verification
- 20 JVM tests: `DoomScrollDetectorTest` (9), `WatchEngineTest` (7), `TrackerAndStatsTest` (4).
- Invariant test `WatchEngineTest.longSessionStartsThrottleAndCooldownStopsIt`: 19 min no start, 20 min one start with the right config, 4 min away no stop, 5 min away one stop.
- Emulator E2E with real YouTube: threshold 5 min, release 1 min. The throttle started on its own after about 5 min, covered only the YouTube uid, and ended 1 to 2 min after leaving.
- Found by E2E and fixed: opening the app did not restart the watcher after an install over an old build.

## Not verified
Quiet hours on a real clock, a 20 min session, a physical phone, release build with R8.

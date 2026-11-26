# Tasks: doom-scroll detection

- [x] 1. `DoomScrollDetector` pure state machine with tests (session, gap, threshold, cooldown, suppression)
- [x] 2. `ForegroundTracker` and `UsageEventsReader`
- [x] 3. `WatchEngine` and stats (today, weekly)
- [x] 4. `ThrottleCoordinator`: auto start/stop, manual timer, notification Stop, VPN revoked
- [x] 5. Invariant test: 19 min no start, 20 min start, 4 min away no stop, 5 min away stop
- [x] 6. `WatcherService` (foreground), `Notifier`, `BootReceiver`, `ActionReceiver`
- [x] 7. DataStore settings and stats repositories
- [x] 8. UI: onboarding with permissions, Home, Stats, Settings, Watched apps
- [x] 9. Redesign from a Stitch export (`docs/stitch`, `docs/REPORT-redesign.md`)
- [x] 10. E2E on emulator with real YouTube, threshold 5 min (`docs/REPORT-e2e.md`)
- [x] 11. Fix: start the watcher when the app opens and Watching is on
- [x] 12. Fix: stale VPN notification after process death
- [x] 13. Reboot test: `BootReceiver` restarts the watcher
- [ ] 14. Quiet hours check on a real clock
- [ ] 15. Physical phone run
- [ ] 16. Signed, minified release build

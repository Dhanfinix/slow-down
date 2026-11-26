# Structure

```
.
├── app/                         Android app (Compose UI, detection, settings)
│   └── src/main/java/com/slowdown/app/
│       ├── ui/                  home, stats, settings, apps, onboarding, theme, common
│       ├── logic/               DoomScrollDetector, WatchEngine, ForegroundTracker, ThrottleCoordinator
│       ├── watch/               WatcherService, UsageEventsReader, Notifier, receivers
│       └── data/                DataStore repositories (settings, stats, apps, permissions)
├── throttle/                    Android library, no UI, the VPN throttle engine
│   └── src/main/java/com/slowdown/throttle/
│       ├── core/                Checksum, Packets, TokenBucket, TcpSession (pure Kotlin)
│       ├── engine/              RelayEngine, NioRemote
│       └── ThrottleApi.kt, ThrottleControllers.kt, SlowDownVpnService.kt
├── docs/                        REPORT-*.md (facts and measurements), screens, stitch export
├── gradle/libs.versions.toml    Version catalog
└── .kiro/                       specs, steering, hooks
```

## Rules
- `:app` depends on `:throttle`. Never the other way.
- Pure logic goes in `core/` or `logic/` with no Android imports, so it can be tested on the JVM.
- Tests sit next to the module in `src/test/java`, one test class per unit.
- Facts about what works go in `docs/REPORT-*.md`, with commands and numbers. Reports say what was not checked.
- Screenshots used in the README live in `docs/readme/`.

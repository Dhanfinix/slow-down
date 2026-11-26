# How I used Kiro

Draft. Facts below come from this project's reports in `docs/`. Items marked `TODO(user)` need your own experience. Delete this line before submitting.

## The project in one line
Slow Down slows the phone's internet through a local VPN when you doom scroll, reviving dial-up and 2G speeds as a wellbeing tool.

## Kiro Crew: a conductor and workers
I did not write the code in one long chat. I used Kiro Crew:

- A **conductor** session held the goal and split it into work items.
- Each item went to its own **worker** session with a time budget. A worker reads its item (`work_brief`), reports progress at milestones (`work_report`), and ends with a `done` claim and evidence.
- A **work ledger** recorded each item and its acceptance bar (for example, "this report file exists"). The conductor checked the claim, it did not take the worker's word.

Items in this project:

| Item | Result |
|---|---|
| Throttle engine (`:throttle`), 90 min budget | Userspace VPN packet relay. 22 JVM tests. 403.6 kbps average at a 400 kbps limit in a loopback test. Report: `docs/REPORT-engine.md` |
| App (`:app`), 120 min budget | Compose UI, detector, watcher, DataStore. 20 more tests. Report: `docs/REPORT-app.md` |
| Independent E2E verifier | Ran everything on an emulator. Report: `docs/REPORT-e2e.md` |
| Redesign from a Stitch export | Restyled the app. Reports: `docs/REPORT-redesign.md`, `docs/REPORT-redesign-verify.md` |
| This submission prep | License, `.kiro`, README, this file |

### What the independent verifier caught
The engine worker could only compile the VPN code. It had never run on a device (its report says so). A separate verifier worker then ran it on an emulator and found:

1. **IPv6 broke HTTPS.** The first design gave the VPN an IPv6 route and answered IPv6 with ICMPv6 "unreachable". Chrome showed `ERR_CONNECTION_REFUSED` and a Java client hung. Unit tests could not see this. The fix was to remove the IPv6 route, so IPv6 fails at once and apps use IPv4.
2. **Watcher did not restart after an update.** After installing over an old build, the watcher service was not running, so detection never started. Fix: `MainActivity` starts it when Watching is on.
3. **Stale notification after a crash.** If the process died during a throttle, the "Internet limited" notification stayed. Fix in `ThrottleControllers`.

Splitting build and verify between different workers mattered. The builder's tests passed while the real app failed.

### Limits I hit
- Kiro Crew's security scan blocked one very large shell heredoc, so files had to be written one at a time with the file tool.
- One worker turn was interrupted. When it resumed, its item was already closed and the new report was refused.

## Specs
`.kiro/specs/` has two specs with EARS-style requirements, design and tasks: `throttle-engine` and `doom-scroll-detection`. They were written from what was built and measured, and open tasks are left unchecked (extra latency test, physical phone). TODO(user): say whether you wrote the specs before or after the code, and what you would change.

## Steering
`.kiro/steering/` has `product.md` (the idea and tone), `tech.md` (stack, commands, rules) and `structure.md` (module layout). Rules that changed behavior:
- One Gradle command at a time, because the machine had little memory.
- Never claim the throttle works without a measurement.
- The `:throttle` public API is a contract, because the app worker built against it in parallel.
- Never commit without the owner's go-ahead.

## Hooks
`.kiro/hooks/` has two files in the v1 hook format:
- `test-throttle-on-save.json`: runs `./gradlew :throttle:testDebugUnitTest` when a `throttle/**/*.kt` file is saved.
- `lint-reminder-app.json`: after an `app/**/*.kt` save, asks the agent to run app tests and lint. A `Stop` hook asks it to name the evidence behind any "it works" claim.

TODO(user): say whether the hooks ran in the IDE or CLI and what they caught.

## Not covered here
TODO(user): MCP servers you used (if any). Powers (if any). Property-based testing (only if you used it; this project's tests are plain JUnit). IDE versus CLI experience. Vibe coding versus spec-driven comparison. Time spent. What surprised you.

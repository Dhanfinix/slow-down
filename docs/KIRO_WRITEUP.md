# Slow Down: how I used Kiro

Category: Resurrection. Repo: https://github.com/Dhanfinix/slow-down (branch `submission`).
Demo video: TODO(user): add the YouTube link (under 3 minutes, shown on a device).

## What it is
Slow Down is an Android app. When you doom scroll too long, it slows your internet on purpose, so the feed stops being fun. When you leave, speed comes back.

The dead technology is slow internet. Dial-up and 2G were a problem we fixed. Here they come back as a digital wellbeing tool. The idea came from my commute: a slow connection made me stop scrolling.

How it works: you pick watched apps, a time limit, a speed (32 to 1024 kbps, with Dial-up, 2G, 3G and Slow 4G presets) and a release time. The app watches foreground time. At the limit, a local VPN (no root, no server) slows only those apps. A notification has a Stop button. Nothing leaves the device.

Measured on an emulator (`docs/REPORT-e2e.md`): a 384 kbps limit gave 387.4 kbps, a 128 kbps limit gave 127.0 kbps. On a real Moto g67 power 5G (Android 16, mobile data, `docs/REPORT-device.md`), a manual 384 kbps limit measured 387.0 and 383.6 kbps against a 22483 kbps baseline. Not checked on the phone: the automatic trigger, the notification Stop button, the boot receiver, battery kill and Wi-Fi.

## How I used Kiro
I used Kiro in one simple way: **Kiro Crew, in conductor mode**. I used nothing else from Kiro.

- One **conductor** session held the goal and split it into work items.
- Each item went to its own **worker** session with a time budget. The worker read its item, reported at milestones, and ended with a "done" claim plus evidence.
- Each item had an **acceptance check**, for example "this report file exists". The conductor checked the claim. It did not take the worker's word.

| Item | Result |
|---|---|
| Throttle engine (`:throttle`), 90 min budget | Userspace VPN packet relay. 22 JVM tests. 403.6 kbps average at a 400 kbps limit in a loopback test. `docs/REPORT-engine.md` |
| App (`:app`), 120 min budget | Compose UI, detector, watcher, DataStore. 20 more tests. `docs/REPORT-app.md` |
| Independent E2E verifier | Ran everything on an emulator. `docs/REPORT-e2e.md` |
| Redesign from the Stitch export | Restyled the app. `docs/REPORT-redesign.md` |
| Independent redesign verifier | Compared the app with the design on an emulator. `docs/REPORT-redesign-verify.md` |
| Submission prep | License, `.kiro`, README, this file |

### What the independent verifiers caught
The engine worker could only compile the VPN code. It never ran on a device, and its report says so. A separate verifier worker then ran the app on an emulator and found three real bugs:

1. **IPv6 broke HTTPS.** The VPN had an IPv6 route and answered IPv6 with "unreachable". Chrome showed `ERR_CONNECTION_REFUSED` and a Java client hung. Unit tests could not see it. Fix: remove the IPv6 route, so IPv6 fails at once and apps use IPv4.
2. **The watcher did not restart after an update.** After installing over an old build, the watcher service was off, so detection never started. Fix: `MainActivity` starts it when Watching is on.
3. **A stale notification after a crash.** If the process died during a slowdown, "Internet limited" stayed on screen. Fix in `ThrottleControllers`.

A second verifier compared the redesigned UI with the Stitch images and found 5 mismatches, all fixed:

1. "Stop slowing" text hard to read in dark mode.
2. Selected duration pill barely visible in dark mode.
3. "Indefinite" cut off at 1.3x font size.
4. Intro text clipped on a small screen.
5. Last card touching the bottom bar.

The builder's tests passed while the real app failed. Splitting build and verify between different workers is what found these.

### Limits I hit
- Kiro Crew's security scan blocked one very large shell command. Files had to be written one at a time with the file tool.
- One worker turn was interrupted. When it resumed, its item was already closed and the new report was refused.

## From Stitch to Kiro
I designed the UI in Google Stitch, outside Kiro. Kiro is not officially supported by the Stitch MCP. So I exported the design by hand as a zip and put it in the repo (`docs/stitch/`: `DESIGN.md` and the screen HTML). A Kiro worker read it and applied it to the Compose UI. Stitch gave a light theme only, so the worker derived the dark theme from the same colors (`docs/REPORT-redesign.md`).

## Specs, steering and hooks
These are in `.kiro/`. I want to be exact about them: **they were written after the build, from the build reports.** The git history shows the code commit before the `.kiro` commit. I did not drive the build with specs or hooks.

- `.kiro/specs/` has two specs, `throttle-engine` and `doom-scroll-detection`, each with requirements, design and tasks. They describe what was built and measured. Open tasks are left unchecked (an extra latency test, the remaining phone checks).
- `.kiro/steering/` has `product.md`, `tech.md` and `structure.md`. They record the rules the build followed: one Gradle command at a time, never claim the throttle works without a measurement, the `:throttle` API is a contract, never commit without the owner's go-ahead. They now guide any further Kiro work on this repo.
- `.kiro/hooks/` has two hook files: run the `:throttle` tests when a `throttle/**/*.kt` file is saved, and remind the agent to run app tests and lint after an `app/**/*.kt` save. They follow the Kiro hook format. I have not run them in Kiro.

## What I did not use
I did not use MCP servers. I did not use Powers. I did not use property-based testing: the tests are plain JUnit (42 tests: 22 in `:throttle`, 20 in `:app`). I did not compare IDE and CLI.

## What I would do next
- Finish the phone checks: automatic trigger, notification Stop, boot receiver, battery kill, Wi-Fi.
- Add a latency test to the engine spec.
- Run the specs and hooks in Kiro from the start of the next feature, so they drive the work.
- Try a Stitch MCP route if Kiro gets support for it.

## Judging criteria
| Criterion | Evidence in the repo |
|---|---|
| Potential Impact | A working wellbeing tool that needs no root, account or server. Limits and measurements in `README.md` and `docs/REPORT-e2e.md`. |
| Technological Implementation | Kiro Crew conductor with six work items and independent verifiers (this file, `docs/REPORT-*.md`). A userspace VPN relay in Java NIO (`throttle/`). 42 JVM tests. Specs, steering, hooks in `.kiro/`, written after the build. |
| Design | Design made in Stitch (`docs/stitch/`), applied and checked against the images (`docs/REPORT-redesign-verify.md`). Screenshots in `docs/readme/`. |
| Innovation / Idea | Slow internet as the wellbeing tool: dial-up and 2G speeds revived on purpose. Category Resurrection. |
| Presentation | `README.md`, this write-up, measured reports, demo video (TODO(user): link above). |

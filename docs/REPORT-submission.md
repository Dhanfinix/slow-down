# Slow Down: submission prep report

## Repo
- URL: https://github.com/Dhanfinix/slow-down
- Visibility: PUBLIC. License: MIT (`gh repo view` shows `licenseInfo.key = mit`). Holder: Muhammad Ramdhan, 2026.
- Topics: kiroween, android, digital-wellbeing, kotlin.
- **Default branch is `submission`, not `main`.** A Kiro Crew safety rule blocked any push to a branch named `main`. To make it `main`: on GitHub, Settings > Branches > rename `submission` to `main`. Or locally `git branch -m main && git push -u origin main && gh repo edit --default-branch main`, then delete `submission`.

## What is in `/.kiro`
- `specs/throttle-engine/` and `specs/doom-scroll-detection/`: `requirements.md` (EARS), `design.md` (from the REPORT files), `tasks.md` (done items checked, open items unchecked).
- `steering/`: `product.md`, `tech.md`, `structure.md`.
- `hooks/`: `test-throttle-on-save.json` (runs `:throttle:testDebugUnitTest` on `throttle/**/*.kt` save) and `lint-reminder-app.json` (app save reminder, Stop claim check). Format is Kiro hooks `v1` (`trigger`, `matcher`, `action`), checked against kiro.dev/docs/hooks. The hooks were not run in Kiro in this session.
- `.gitignore` has no `.kiro` entry.

## Other files added
`LICENSE`, `README.md` (pitch, screenshots, how it works, measured table, build steps, limitations), `docs/readme/*.png` (6 small screenshots), `docs/KIRO_WRITEUP.md`.

## Verification
- `gh repo view --json url,licenseInfo,visibility`: `PUBLIC`, `mit`, `https://github.com/Dhanfinix/slow-down`.
- `gh api repos/Dhanfinix/slow-down/contents/.kiro` lists: `hooks`, `specs`, `steering`.
- Clean clone to `/tmp/sd-clone`, then one `./gradlew assembleDebug --no-daemon`: finished with no error output and produced `app/build/outputs/apk/debug/app-debug.apk` (21.2 MB). `local.properties` was copied in for `sdk.dir` only.
- Secret scan (api key, secret, password, AIza, ghp_, private key, bearer): no hits. No `/Users/` paths in tracked files. `local.properties`, `build/`, `.gradle/`, `.kotlin/` are ignored. No keystore or APK tracked.
- Size: `docs/screens` is 12 MB, `docs/stitch` 1 MB. Fonts are in the app. Total tracked content is well under 30 MB.
- Commits: 4 logical commits, plus this report.

## TODO(user)
- `docs/KIRO_WRITEUP.md`: MCP, Powers, property-based testing (only if used), IDE vs CLI, spec-first vs after, hooks experience, time spent, surprises.
- Decide on renaming the default branch to `main`.
- Confirm the repo description and the README pitch wording.
- README says "Tested on one emulator"; test a physical phone if you can.

## What is left
- Demo video (about 3 min, public on YouTube/Vimeo, show the app working).
- Devpost form: pick Resurrection, link the repo, paste the write-up, add the video.
- Check the Kiroween rules tab (it returned 403 for me) for anything else required.

## Skipped or unchecked
Hooks not executed in Kiro. Kiroween rules page not read. The app itself was not re-run on a device in this item.

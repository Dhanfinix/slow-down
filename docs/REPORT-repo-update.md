# Report: repo update with real-phone results

Branch: `submission` (not `main`). Pushed to https://github.com/Dhanfinix/slow-down.

## Commits
- `7b8beb2` README and write-up: add real-phone results.
- `e5ab1c6` Add real-phone test report and screenshots. This is the head, and `HEAD` equals `origin/submission`.

## Files changed
- `README.md`: tested-on line, phone table (384 kbps gave 387.0 and 383.6; baseline 22483; after Stop 18107), Limitations, Moto battery tip, one phone screenshot.
- `docs/KIRO_WRITEUP.md`: device-test lines only.
- `docs/readme/phone-slowed.png`: new, 145 KB, status bar cropped out (it showed other apps' notification icons).
- `docs/REPORT-device.md` and `docs/screens/device-01` to `device-10` (2.0 MB total). All were under 250 KB, so none were resized. `device-08-settings-scope.png` is a valid PNG, so it was kept.

## Checked
`gh api repos/Dhanfinix/slow-down/contents/docs/REPORT-device.md?ref=submission` returns the file.

## Not checked
Rendering of the README on GitHub was not looked at. No app code was changed.

## Still for the user
- Record the demo video and put the link in `docs/KIRO_WRITEUP.md` (the TODO line).
- Fill in the Devpost form.
- Not yet tested on the phone: automatic doom-scroll trigger, notification Stop, boot receiver, cooldown, Moto battery kill, Wi-Fi.
- The device screenshots in `docs/screens/` show other apps' notification icons in the status bar. Delete or crop them if that matters.

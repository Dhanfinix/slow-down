# Requirements: doom-scroll detection (`:app`)

Goal: notice when the user has stayed in a watched app too long, then slow the network, and release it after the user leaves.

## Requirements

1. **Foreground detection**
   - 1.1 WHILE Watching is on, THE watcher SHALL poll `UsageStatsManager` events every 15 s and track the foreground package.
   - 1.2 WHEN the screen turns off, THE tracker SHALL treat the app as no longer in the foreground.
   - 1.3 WHERE usage access is not granted, THE app SHALL only offer manual slow-down.

2. **Session**
   - 2.1 WHEN a watched app is first seen, THE detector SHALL start a session.
   - 2.2 WHEN the user leaves the watched app for less than 2 minutes, THE session SHALL continue.
   - 2.3 WHEN ticks were missed (service killed, doze), THE session SHALL end.

3. **Start**
   - 3.1 WHEN a session reaches the threshold (5 to 120 min, user setting), THE detector SHALL return START, unless quiet hours apply or the user already stopped this session.
   - 3.2 WHEN START happens, THE coordinator SHALL start the throttle at the chosen speed (32 to 1024 kbps) for the watched apps, or for all apps when scope is Whole phone, and post "You have been scrolling N min. Slowing down." with a Stop action.

4. **Stop**
   - 4.1 WHILE throttling, WHEN the user is away for the release time (1 to 30 min), THE detector SHALL return STOP and the coordinator SHALL stop the throttle.
   - 4.2 WHEN the user returns inside the release time, THE throttle SHALL stay on.
   - 4.3 WHEN the user presses Stop in the notification, THE coordinator SHALL stop and the detector SHALL NOT restart in the same session.
   - 4.4 WHEN the VPN is revoked or errors, THE session SHALL close and stats SHALL be saved.

5. **Manual mode**
   - 5.1 THE detector SHALL NOT end a manual session. A manual timer (15, 30, 60 min or none) ends it.

6. **Resilience**
   - 6.1 WHEN the device reboots with Watching on, THE `BootReceiver` SHALL restart the watcher, or post "Resume watching?" if Android refuses.
   - 6.2 WHEN the app opens and Watching is on but the service is not running, THE app SHALL start it.

7. **Data**
   - 7.1 THE app SHALL keep settings and stats in DataStore only and SHALL NOT send them off the device.

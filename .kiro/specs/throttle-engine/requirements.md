# Requirements: throttle engine (`:throttle`)

Goal: limit the internet speed of selected apps on a phone without root, using a local `VpnService`. No traffic leaves the device for a third party.

Glossary: **TUN** is the virtual network interface the VPN gives us. **Relay** is our code that reads packets from the TUN and talks to the real network.

## Requirements

1. **Start and stop**
   - 1.1 WHEN `ThrottleController.start(config)` is called AND VPN consent was granted, THE engine SHALL establish a VPN and set state to `Active`.
   - 1.2 WHEN `start()` is called without VPN consent, THE engine SHALL set state to `Error("VPN permission not granted")`.
   - 1.3 WHEN `stop()` is called, THE engine SHALL tear down the VPN and set state to `Idle`.
   - 1.4 WHEN the user turns the VPN off in system UI (`onRevoke`), THE engine SHALL stop and set state to `Idle`.
   - 1.5 WHEN the app process is killed during a throttle, THE next process SHALL remove the stale VPN notification.

2. **Speed limit**
   - 2.1 WHILE state is `Active`, THE engine SHALL limit TCP download to `downKbps` and upload to `upKbps`.
   - 2.2 THE measured average download speed SHALL be within 5% of the limit over a 10 s test.
   - 2.3 WHEN `updateSpeed(kbps)` is called while `Active`, THE engine SHALL apply the new rate to both directions within about 100 ms.
   - 2.4 WHEN a download exceeds the limit, THE engine SHALL slow it with TCP flow control (stop reading, shrink the window) and SHALL NOT drop TCP data.
   - 2.5 WHEN UDP traffic exceeds the limit, THE engine SHALL drop the extra packets. DNS (port 53) SHALL NOT be rate limited.

3. **Scope**
   - 3.1 WHERE `targetPackages` is not empty, ONLY those apps SHALL go through the VPN.
   - 3.2 WHERE `targetPackages` is empty, ALL apps SHALL be throttled except Slow Down itself and `excludedPackages`.

4. **Compatibility**
   - 4.1 WHEN a covered app opens an IPv6 connection, THE engine SHALL let it fail at once so the app falls back to IPv4 (no IPv6 route in the VPN).
   - 4.2 WHEN the real connect for a TCP flow fails, THE engine SHALL answer the app with RST.
   - 4.3 HTTPS (including DNS and TLS) SHALL work while throttled.

5. **Resources**
   - 5.1 THE engine SHALL cap sessions at 1024 TCP and 512 UDP flows, and close idle flows (TCP 5 min, UDP 60 s, DNS 15 s).

6. **Public API stability**
   - 6.1 THE public API (`ThrottleController`, `ThrottleConfig`, `ThrottleState`, `ThrottleControllers`, `VpnPermission`, `SlowDownVpnService`) SHALL NOT change without updating `:app`.

7. **Honesty**
   - 7.1 THE project SHALL NOT claim the throttle works unless a measured number backs it (see `docs/REPORT-e2e.md`).

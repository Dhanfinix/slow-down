# Slow Down: throttle engine report

## Toolchain
AGP 8.13.0, Kotlin 2.2.21, Gradle 8.14.2 (wrapper), JDK 18 (Temurin, `/usr/bin/java`), compileSdk 36, minSdk 26, targetSdk 36.
Compose BOM 2025.10.01 (material3 1.4.0, which has the expressive APIs), DataStore 1.1.7, coroutines 1.10.2. Version catalog: `gradle/libs.versions.toml`.

Modules: `:app` (placeholder `MainActivity` only, depends on `:throttle`), `:throttle` (Android library, no UI).

## Commands run and results
Run from the project root, `--no-daemon`, one at a time.

```
./gradlew :throttle:testDebugUnitTest --no-daemon --rerun-tasks
  > Task :throttle:testDebugUnitTest
  BUILD SUCCESSFUL in 22s

./gradlew assembleDebug --no-daemon
  BUILD SUCCESSFUL in 8s   (APK: app/build/outputs/apk/debug/app-debug.apk, 11.9 MB)
```

Test result files (`throttle/build/test-results/testDebugUnitTest/`): 22 tests, 0 failures, 0 errors.

| Class | Tests | Covers |
|---|---|---|
| `CoreTest` | 9 | RFC 1071 checksum vector and odd length. IPv4/TCP/UDP build, parse and checksum round trip. MSS option parse. Truncated packet rejected. ICMPv6 unreachable checksum. Token bucket rate (10 s, +-5/+12%), burst cap, refund |
| `TcpSessionTest` | 11 | Handshake (SYN-ACK, MSS 1360, ack number). Upload data and ack. Duplicate and overlapping data. Download segmentation and client window. Download rate +-20% at 800 kbps over 12 s (fake clock). Upload rate and window shrink (80 kbps). FIN both orders. Connect failure RST. Client RST. Remote error RST |
| `RelayEngineLoopbackTest` | 2 | Real NIO sockets on 127.0.0.1 with a fake TUN and a mini client stack. TCP download for 10 s at 400 kbps. UDP echo round trip |

Measured by the loopback test (real `RelayEngine`, real sockets): **403.6 kbps average over 10 s at a 400 kbps limit** (504,560 bytes, no loss, in order). The connection completed the handshake and moved data both ways.

## Public API as implemented
Package `com.slowdown.throttle`.

```kotlin
interface ThrottleController {
    val state: StateFlow<ThrottleState>
    fun start(config: ThrottleConfig)
    fun stop()
    fun updateSpeed(kbps: Int)   // sets BOTH down and up to kbps, only while running
}
data class ThrottleConfig(
    val downKbps: Int, val upKbps: Int, val extraLatencyMs: Int = 0,
    val targetPackages: Set<String> = emptySet(),   // empty = all apps
    val excludedPackages: Set<String> = emptySet(),
)
sealed interface ThrottleState {
    data object Idle; data object Starting
    data class Active(val downKbps: Int, val upKbps: Int, val bytesDown: Long, val bytesUp: Long)
    data class Error(val message: String)
}
object ThrottleControllers { fun get(context: Context): ThrottleController }   // process singleton
object VpnPermission { fun prepareIntent(context): Intent?; fun isGranted(context): Boolean }
class SlowDownVpnService : VpnService()
```

Usage for the UI worker:
1. `VpnPermission.prepareIntent(ctx)`: if not null, launch it with the Activity Result API and wait for `RESULT_OK`.
2. On Android 13+, request `POST_NOTIFICATIONS` (declared in the library manifest, not requested by the library).
3. `ThrottleControllers.get(ctx).start(ThrottleConfig(downKbps = 200, upKbps = 200))`. Observe `state`.
4. `start()` without permission sets `Error("VPN permission not granted")`. Calling `start()` again restarts with the new config.
5. `Active` is re-emitted about once a second with byte counters (bytes of TCP/UDP payload).

Behaviour notes:
- Per-app: if `targetPackages` is non-empty only those apps go through the VPN (`addAllowedApplication`) and `excludedPackages` is ignored (Android forbids mixing). If empty, all apps are throttled except Slow Down itself and `excludedPackages`.
- Service: declared in the `:throttle` manifest with `BIND_VPN_SERVICE`, `foregroundServiceType="specialUse"` (subtype property `vpn_network_throttling`), permissions `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`. Notification channel `slowdown_throttle`, ongoing notification with a Stop action. `onRevoke()` (user turns the VPN off in system UI) stops the engine and sets `Idle`.
- Why `specialUse`: `systemExempted` is for system-granted cases and needs special approval. `specialUse` is the documented type for a VPN app that is not the system VPN settings UI. For Play Store release a "special use" declaration is needed.

## Engine design
- `core/` (pure Kotlin, JVM tests): `Checksum`, `Packets` (IPv4/TCP/UDP parse and build, ICMPv6), `TokenBucket`, `TcpSession`.
- `engine/RelayEngine`: one TUN reader thread feeds a queue. One engine thread owns the NIO `Selector`, all TCP sessions and UDP flows. Each flow uses its own `protect()`-ed non-blocking socket.
- TCP: the session plays the server for the app. The SYN-ACK is sent only after the real connect succeeds. Failed connect gives RST. MSS 1360 (TUN MTU 1400). No window scaling or SACK offered (windows are max 64 KB).
- Download limit: bytes are read from the real socket only when the shared download bucket has tokens and the app's window has room. The real server is slowed by normal TCP flow control, nothing is dropped.
- Upload limit: app data goes in a 64 KB buffer. It drains into the real socket at the upload rate. The advertised window shrinks while the buffer is full, so the app slows down.
- UDP: relayed per (src port, dst ip, dst port). Over-limit UDP is dropped (QUIC then backs off). DNS (port 53) is not rate limited. Idle: 15 s for DNS, 60 s others.
- IPv6: the TUN has an IPv6 address and `::/0` route so v6 traffic cannot leak around the throttle. TCP/UDP over v6 gets an immediate ICMPv6 destination unreachable, so apps fall back to IPv4 fast.
- Extra latency: `extraLatencyMs` is split in two halves, one on the way into the relay and one on the way back into the TUN, through a delay queue (order kept).
- Limits: 1024 TCP sessions, 512 UDP flows, TCP idle 5 min, connect timeout 20 s.
- Token bucket: burst = max(rate/10, 3000 B), so a full segment always fits.

## Known limitations
- **Not tested on a device or emulator.** `VpnService`, `establish()`, `protect()`, the foreground service, the notification and the manifest were only compiled (`assembleDebug`), never run. The next worker must do emulator E2E.
- No TCP retransmit queue. The TUN is lossless, so this is fine unless the app's stack drops a packet. A dropped segment would stall that connection until idle timeout. Not seen in tests because the test client never drops.
- No window scaling: one connection's max speed is 64 KB / RTT. Fine for throttled speeds. Unthrottled use of a high speed link may be slower than native.
- IP fragments are dropped. ICMP (ping) is not relayed. IPv6 is rejected, not relayed.
- UDP replies larger than the MTU are written to the TUN as is.
- DNS servers are not set in the VPN builder, so Android uses the underlying network's DNS and the queries are relayed. Not checked on a device. If name lookups fail on device, add `addDnsServer(...)` in `SlowDownVpnService.establish`.
- Throttle accuracy applies to payload bytes of TCP downloads. Header bytes are not counted, so wire speed is about 3 to 5% above the setting.
- `updateSpeed(kbps)` changes both directions to the same value. The bucket keeps its current tokens, so a change takes effect within about 100 ms.
- Extra latency was not covered by a test.
- Service state hand-off uses a process-wide singleton (service and controller must run in one process, which is the default).
- Leftover in `TcpSessionTest`: the fake clock tests step 1 ms, so they model the engine polling every 5 ms or less.

## How to verify on a device
1. Install: `./gradlew :app:installDebug` (needs the UI worker's start button, or call `ThrottleControllers.get(ctx).start(...)` from a debug hook after granting VPN consent).
2. Grant the VPN dialog. Check `adb shell dumpsys connectivity | grep -i vpn` shows the VPN and the notification appears.
3. Start with `downKbps = 1000`. On the device run a download for at least 10 s, for example `adb shell "curl -o /dev/null -w '%{speed_download}\n' https://speed.cloudflare.com/__down?bytes=50000000"` (speed is bytes per second, expect about 125000), or open a video.
4. Open an HTTPS site and a QUIC site (Chrome). Both should load. `adb logcat -s SlowDownEngine SlowDownVpn` for errors.
5. Change speed with `updateSpeed(200)` and repeat step 3 (expect about 25000 B/s).
6. Stop from the notification and confirm normal speed and the VPN key icon gone.
7. Check IPv6 only sites fail fast and IPv4 sites work.

## What I skipped
- Any run on a real device or emulator.
- A retransmit queue, window scaling, SACK, IP fragment reassembly, ICMP relay.
- Test for `extraLatencyMs`, for `updateSpeed` while connections are running, and for many parallel connections (the shared bucket is only checked with one).
- Lint run.

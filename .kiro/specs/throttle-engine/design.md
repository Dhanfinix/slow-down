# Design: throttle engine

Source of truth: `docs/REPORT-engine.md` and `docs/REPORT-e2e.md`.

## Approach
A userspace relay. The VPN routes covered apps' IPv4 traffic into a TUN. We read raw IP packets, act as the server for the app's TCP stack, and open our own real socket (`protect()`ed so it bypasses the VPN) to the real server. Because we sit in the middle, we choose how fast bytes move.

```
app -> TUN -> RelayEngine -> protect()ed NIO socket -> internet
app <- TUN <- RelayEngine <- protect()ed NIO socket <- internet
```

## Components (package `com.slowdown.throttle`)
| Part | Job |
|---|---|
| `core/Checksum`, `core/Packets` | IPv4/TCP/UDP parse and build, RFC 1071 checksum, ICMPv6 |
| `core/TokenBucket` | Rate limit. Burst = max(rate/10, 3000 B) so one full segment always fits |
| `core/TcpSession` | Plays the server for the app. SYN-ACK only after the real connect succeeds. MSS 1360 (TUN MTU 1400). No window scaling or SACK |
| `engine/RelayEngine` | One TUN reader thread feeds a queue. One engine thread owns the NIO `Selector`, all TCP sessions and UDP flows |
| `engine/NioRemote` | The real socket for one flow |
| `SlowDownVpnService` | `VpnService`, foreground (`specialUse`), notification with Stop action |
| `ThrottleControllers` | Process singleton that the app uses |

## Key decisions
- **Download limit:** read from the real socket only when the shared bucket has tokens and the app's window has room. TCP flow control slows the real server. Nothing is dropped.
- **Upload limit:** app data goes into a 64 KB buffer that drains at the upload rate. The advertised window shrinks while it is full.
- **UDP:** relayed per (src port, dst ip, dst port). Over-limit packets are dropped so QUIC backs off.
- **IPv6:** no address and no route in the VPN. First design returned ICMPv6 unreachable. E2E showed Chrome failed with `ERR_CONNECTION_REFUSED` and Java clients hung, so the route was removed. IPv6 then fails at once and apps use IPv4.
- **Foreground service type:** `specialUse` (subtype `vpn_network_throttling`). `systemExempted` needs special approval.
- **Per-app:** `addAllowedApplication` when targets exist (Android forbids mixing allowed and disallowed lists).
- **Extra latency:** split in two halves through a delay queue. Implemented, not tested.

## Verification
- 22 JVM tests in `:throttle`: `CoreTest` (9), `TcpSessionTest` (11), `RelayEngineLoopbackTest` (2, real sockets, fake TUN). Loopback: 403.6 kbps average at a 400 kbps limit over 10 s.
- Emulator E2E (API 35): 128 kbps gave 127.0, 384 gave 387.4 and 388.1, 1024 gave 1012.7. Baseline 29833 kbps. Error within 1.5%.

## Known limits
No TCP retransmit queue. No window scaling (one stream tops out at 64 KB / RTT). IP fragments dropped. ICMP not relayed. IPv6-only networks do not work. Private DNS with a hostname is untested. Extra latency and QUIC are untested.

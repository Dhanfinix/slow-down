# Tasks: throttle engine

- [x] 1. Gradle project, `:app` skeleton, `:throttle` Android library, version catalog
- [x] 2. `core/Checksum` and `core/Packets` with tests (RFC 1071 vector, round trips, truncated packet)
- [x] 3. `core/TokenBucket` with rate, burst and refund tests
- [x] 4. `core/TcpSession`: handshake, data both ways, FIN, RST, rate tests with a fake clock
- [x] 5. `engine/RelayEngine` and `NioRemote`: selector loop, TCP and UDP relay
- [x] 6. Loopback test with real sockets and a fake TUN (403.6 kbps at 400 kbps)
- [x] 7. `SlowDownVpnService`, notification with Stop, `onRevoke`
- [x] 8. Public API: `ThrottleController`, `ThrottleConfig`, `ThrottleState`, `ThrottleControllers`, `VpnPermission`
- [x] 9. Emulator E2E: VPN consent, three speeds measured, Chrome HTTPS (`docs/REPORT-e2e.md`)
- [x] 10. Fix: remove IPv6 address and route (Chrome `ERR_CONNECTION_REFUSED`)
- [x] 11. Fix: clear stale VPN notification after process death
- [ ] 12. Test `extraLatencyMs`
- [ ] 13. Test many parallel connections and QUIC
- [ ] 14. Test Private DNS with a hostname; consider `addDnsServer`
- [ ] 15. Run on a physical phone

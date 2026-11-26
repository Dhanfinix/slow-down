package com.slowdown.throttle

import kotlinx.coroutines.flow.StateFlow

interface ThrottleController {
    val state: StateFlow<ThrottleState>

    /** Starts (or restarts with a new config) the VPN throttle. The VPN permission must already be granted, see [VpnPermission]. */
    fun start(config: ThrottleConfig)
    fun stop()

    /** Sets both download and upload speed to [kbps] (kilobits per second) while running. */
    fun updateSpeed(kbps: Int)
}

data class ThrottleConfig(
    val downKbps: Int,
    val upKbps: Int,
    val extraLatencyMs: Int = 0,
    /** Empty means all apps. */
    val targetPackages: Set<String> = emptySet(),
    val excludedPackages: Set<String> = emptySet(),
)

sealed interface ThrottleState {
    data object Idle : ThrottleState
    data object Starting : ThrottleState
    data class Active(val downKbps: Int, val upKbps: Int, val bytesDown: Long, val bytesUp: Long) : ThrottleState
    data class Error(val message: String) : ThrottleState
}

package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.CounterType
import kotlinx.serialization.Serializable

/** Conditions and riders for an optional opening-hand battlefield action. */
@Serializable
data class OpeningHandBattlefieldOptions(
    val requireNotStartingPlayer: Boolean = false,
    val entryCounters: Map<CounterType, Int> = emptyMap(),
    val exileFromHandCount: Int = 0
) {
    init {
        require(exileFromHandCount >= 0)
        require(entryCounters.values.all { it >= 0 })
    }
}

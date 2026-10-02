package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class EntryCounterDestinationTest : FunSpec({
    test("entry counters round trip as a composed destination") {
        val destination: CardDestination = CardDestination.WithEntryCounters(
            CardDestination.ToZone(Zone.BATTLEFIELD),
            mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1, CounterType.CHARGE to 2)
        )
        Json.decodeFromString<CardDestination>(Json.encodeToString(destination)) shouldBe destination
    }
    test("entry counters reject nonbattlefield destinations and negative counts") {
        shouldThrow<IllegalArgumentException> {
            CardDestination.WithEntryCounters(CardDestination.ToZone(Zone.EXILE), emptyMap())
        }
        shouldThrow<IllegalArgumentException> {
            CardDestination.WithEntryCounters(CardDestination.ToZone(Zone.BATTLEFIELD), mapOf(CounterType.CHARGE to -1))
        }
    }
})

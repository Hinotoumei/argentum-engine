package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.Conditions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class IntrinsicCounterProtectionSerializationTest : FunSpec({
    for (ability in listOf<StaticAbility>(CantBeCountered,
        ConditionalStaticAbility(CantBeCountered, Conditions.CardsInHandAtMost(0))))
        test("intrinsic protection round trips: ${ability.description}") {
            Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
        }
})

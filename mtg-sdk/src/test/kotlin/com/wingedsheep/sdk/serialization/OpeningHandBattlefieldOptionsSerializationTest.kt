package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.OpeningHandBattlefieldOptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OpeningHandBattlefieldOptionsSerializationTest : FunSpec({
    val json = CardSerialization.json

    test("opening-hand conditions and riders survive card-script serialization") {
        val script = CardScript(
            mayStartOnBattlefield = true,
            openingHandBattlefieldOptions = OpeningHandBattlefieldOptions(
                requireNotStartingPlayer = true,
                entryCounters = mapOf(CounterType.LUCK to 1),
                exileFromHandCount = 1
            )
        )
        val encoded = json.encodeToString(CardScript.serializer(), script)
        json.decodeFromString(CardScript.serializer(), encoded) shouldBe script
    }

    test("existing opening-hand scripts retain unconditional defaults") {
        val script = json.decodeFromString(CardScript.serializer(), """{"mayStartOnBattlefield":true}""")
        script.mayStartOnBattlefield shouldBe true
        script.openingHandBattlefieldOptions shouldBe OpeningHandBattlefieldOptions()
    }
})

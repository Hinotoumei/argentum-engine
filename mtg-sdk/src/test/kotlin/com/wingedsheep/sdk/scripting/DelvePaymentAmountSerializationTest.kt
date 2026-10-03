package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DelvePaymentAmountSerializationTest : FunSpec({
    for (amount in listOf(DynamicAmounts.cardsExiledForDelve(),
        DynamicAmounts.cardsExiledForDelve(CardType.INSTANT, CardType.SORCERY))) {
        test("delve amount round trips: $amount") {
            Json.decodeFromString<DynamicAmount>(Json.encodeToString(amount)) shouldBe amount
        }
    }
})

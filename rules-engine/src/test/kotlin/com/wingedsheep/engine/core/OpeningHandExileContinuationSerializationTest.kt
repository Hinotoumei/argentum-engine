package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class OpeningHandExileContinuationSerializationTest : FunSpec({
    test("mandatory opening-hand exile survives a suspended game round trip") {
        val json = Json { serializersModule = engineSerializersModule }
        val player = EntityId.of("player")
        val source = EntityId.of("source")
        val original: ContinuationFrame = Suspension(
            question = SelectCardsDecision(
                id = "opening-exile",
                playerId = player,
                prompt = "Exile another card",
                context = DecisionContext(sourceId = source, sourceName = "Opening-hand permanent"),
                options = listOf(EntityId.of("other-card")),
                minSelections = 1,
                maxSelections = 1
            ),
            answer = OpeningHandExileContinuation(player, source, 1, "Opening-hand permanent")
        )
        val encoded = json.encodeToString(ContinuationFrame.serializer(), original)
        json.decodeFromString(ContinuationFrame.serializer(), encoded) shouldBe original
    }
})

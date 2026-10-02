package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.GeierReachSanitarium
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GeierReachSanitariumScenarioTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    val cards = TestCards.all + GeierReachSanitarium
    fun setup() = GameTestDriver().also { d ->
        d.registerCards(cards)
        d.initMultiplayer(List(3) { Deck.of("Island" to 40) }, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (saved in listOf(false, true)) test("all three players draw before hidden APNAP selections and simultaneous owner-routed discards; saved=$saved") {
        val d = setup(); val active = d.activePlayer!!
        val order = d.state.apnapOrder
        val handSizes = order.associateWith { d.getHand(it).size }
        val chosen = order.associateWith { d.putCardInHand(it, "Grizzly Bears") }
        val land = d.putLandOnBattlefield(active, "Geier Reach Sanitarium")
        d.giveMana(active, Color.BLUE, 2)
        d.submit(ActivateAbility(active, land, GeierReachSanitarium.script.activatedAbilities[1].id)).error shouldBe null
        while (d.state.pendingDecision == null && d.stackSize > 0) d.passPriority(d.priorityPlayer!!).error shouldBe null
        for (player in order) d.getHand(player).size shouldBe handSizes.getValue(player) + 2
        val eventStart = d.events.size
        for ((index, player) in order.withIndex()) {
            val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe player
            decision.options.toSet() shouldBe d.getHand(player).toSet()
            if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            // Earlier choices must still be in their hands; no player learns their identities.
            for (prior in order.take(index)) {
                d.getHand(prior).contains(chosen.getValue(prior)) shouldBe true
                d.getGraveyard(prior).contains(chosen.getValue(prior)) shouldBe false
                d.state.getEntity(chosen.getValue(prior))?.get<RevealedToComponent>() shouldBe null
            }
            d.submitCardSelection(player, listOf(chosen.getValue(player))).error shouldBe null
        }
        d.state.pendingDecision shouldBe null
        val events = d.events.drop(eventStart).filterIsInstance<CardsDiscardedEvent>()
        events.associate { it.playerId to it.cardIds } shouldBe chosen.mapValues { listOf(it.value) }
        for (player in order) {
            d.getGraveyard(player).contains(chosen.getValue(player)) shouldBe true
            d.getHand(player).size shouldBe handSizes.getValue(player) + 1
        }
    }
    test("the first ability supplies colorless mana without using the stack") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.putLandOnBattlefield(me, "Geier Reach Sanitarium")
        d.submit(ActivateAbility(me, land, GeierReachSanitarium.script.activatedAbilities[0].id)).error shouldBe null
        d.stackSize shouldBe 0
        d.state.getEntity(me)?.get<ManaPoolComponent>()?.colorless shouldBe 1
    }
    test("unaffordable draw-discard activation leaves the state untouched") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.putLandOnBattlefield(me, "Geier Reach Sanitarium")
        val before = d.state
        d.submit(ActivateAbility(me, land, GeierReachSanitarium.script.activatedAbilities[1].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("a player cannot select a card in another player's hand for their discard") {
        val d = setup(); val me = d.activePlayer!!
        val opponent = d.state.apnapOrder[1]
        val foreign = d.putCardInHand(opponent, "Grizzly Bears")
        val land = d.putLandOnBattlefield(me, "Geier Reach Sanitarium")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(ActivateAbility(me, land, GeierReachSanitarium.script.activatedAbilities[1].id)).error shouldBe null
        var passes = 0
        while (d.state.pendingDecision == null && d.stackSize > 0 && passes++ < 8) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe me
        val before = d.state
        d.submitCardSelection(me, listOf(foreign)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

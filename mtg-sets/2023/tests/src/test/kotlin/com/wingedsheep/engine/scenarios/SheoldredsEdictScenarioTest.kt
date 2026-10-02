package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.SheoldredsEdict
import com.wingedsheep.mtg.sets.definitions.lrw.cards.JaceBeleren
import com.wingedsheep.mtg.sets.definitions.lrw.cards.GarrukWildspeaker
import com.wingedsheep.mtg.sets.definitions.som.cards.DarksteelMyr
import com.wingedsheep.mtg.sets.definitions.gpt.cards.LeylineOfTheVoid
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SheoldredsEdictScenarioTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    val cards = TestCards.all + listOf(SheoldredsEdict, JaceBeleren, GarrukWildspeaker, DarksteelMyr, LeylineOfTheVoid)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMultiplayer(List(4) { Deck.of("Swamp" to 40) }, startingPlayer = 2)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, caster: EntityId, mode: Int) {
        val spell = d.putCardInHand(caster, "Sheoldred's Edict")
        d.giveMana(caster, Color.BLACK, 2)
        d.submit(CastSpell(caster, spell, chosenModes = listOf(mode))).error shouldBe null
        var passes = 0
        while (d.state.pendingDecision == null && d.stackSize > 0 && passes++ < 8) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (passes < 8) shouldBe true
    }
    for (saved in listOf(false, true)) test("opponents choose in APNAP order before any sacrifices; saved=$saved") {
        val d = setup(); val caster = d.activePlayer!!
        val opponents = d.state.apnapOrder.filter { d.state.isOpponentOf(it, caster) }
        val chosen = opponents.associateWith { d.putCreatureOnBattlefield(it, "Grizzly Bears") }
        val survivors = opponents.associateWith { d.putCreatureOnBattlefield(it, "Hill Giant") }
        val own = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        cast(d, caster, 0)
        val start = d.events.size
        for ((index, opponent) in opponents.withIndex()) {
            val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe opponent
            decision.options.toSet() shouldBe setOf(chosen.getValue(opponent), survivors.getValue(opponent))
            for (prior in opponents.take(index)) {
                d.getGraveyard(prior).contains(chosen.getValue(prior)) shouldBe false
                d.state.getBattlefield().contains(chosen.getValue(prior)) shouldBe true
                d.events.drop(start).filterIsInstance<CardsRevealedEvent>()
                    .any { it.revealingPlayerId == prior && it.cardIds == listOf(chosen.getValue(prior)) } shouldBe true
            }
            if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            d.submitCardSelection(opponent, listOf(chosen.getValue(opponent))).error shouldBe null
        }
        d.state.pendingDecision shouldBe null
        for (opponent in opponents) {
            d.getGraveyard(opponent).contains(chosen.getValue(opponent)) shouldBe true
            d.state.getBattlefield().contains(survivors.getValue(opponent)) shouldBe true
        }
        d.state.getBattlefield().contains(own) shouldBe true
        val sacrificed = d.events.drop(start).filterIsInstance<PermanentsSacrificedEvent>()
        sacrificed.associate { it.playerId to it.permanentIds } shouldBe chosen.mapValues { listOf(it.value) }
        d.events.drop(start).filterIsInstance<ZoneChangeEvent>()
            .filter { it.entityId in chosen.values }.all { it.wasSacrificed } shouldBe true
    }
    test("an opponent without an eligible nontoken creature does not stop later opponents choosing") {
        val d = setup(); val caster = d.activePlayer!!
        val opponents = d.state.apnapOrder.filter { d.state.isOpponentOf(it, caster) }
        val victim = d.putCreatureOnBattlefield(opponents.last(), "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(opponents.last(), "Hill Giant")
        cast(d, caster, 0)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponents.last()
        d.submitCardSelection(decision.playerId, listOf(victim)).error shouldBe null
        d.getGraveyard(opponents.last()).contains(victim) shouldBe true
        d.state.getBattlefield().contains(other) shouldBe true
        d.state.pendingDecision shouldBe null
    }
    test("unaffordable casting is rejected without changing state") {
        val d = setup(); val caster = d.activePlayer!!
        val spell = d.putCardInHand(caster, "Sheoldred's Edict")
        val before = d.state
        d.submit(CastSpell(caster, spell, chosenModes = listOf(0))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    for (mode in listOf(0, 1, 2)) test("mode $mode filters nontoken creatures, creature tokens, and planeswalkers independently") {
        val d = setup(); val caster = d.activePlayer!!
        val opponent = d.state.apnapOrder.first { d.state.isOpponentOf(it, caster) }
        val creature = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val otherCreature = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val token = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        d.addComponent(token, TokenComponent)
        val otherToken = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        d.addComponent(otherToken, TokenComponent)
        val walker = d.putPermanentOnBattlefield(opponent, "Jace Beleren")
        val otherWalker = d.putPermanentOnBattlefield(opponent, "Garruk Wildspeaker")
        // The direct battlefield fixture bypasses entry; initialize printed loyalty explicitly.
        d.addComponent(walker, CountersComponent().withAdded(CounterType.LOYALTY, JaceBeleren.startingLoyalty!!))
        d.addComponent(otherWalker, CountersComponent().withAdded(CounterType.LOYALTY, GarrukWildspeaker.startingLoyalty!!))
        val expected = listOf(creature, token, walker)[mode]
        cast(d, caster, mode)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponent
        decision.options.toSet() shouldBe setOf(expected, listOf(otherCreature, otherToken, otherWalker)[mode])
        d.submitCardSelection(opponent, listOf(expected)).error shouldBe null
        d.state.pendingDecision shouldBe null
        for (id in listOf(creature, token, walker)) d.state.getBattlefield().contains(id) shouldBe (id != expected)
    }
    test("the nontoken choice cannot be replaced with a creature token and rejection preserves the pending choice") {
        val d = setup(); val caster = d.activePlayer!!
        val opponent = d.state.apnapOrder.first { d.state.isOpponentOf(it, caster) }
        val creature = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val token = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        d.addComponent(token, TokenComponent)
        cast(d, caster, 0)
        val before = d.state
        d.submitCardSelection(opponent, listOf(token)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.submitCardSelection(opponent, listOf(creature)).error shouldBe null
        d.state.getBattlefield().contains(token) shouldBe true
    }

    test("indestructibility does not prevent sacrifice, and an opponent graveyard replacement redirects it") {
        val d = setup(); val caster = d.activePlayer!!
        val opponent = d.state.apnapOrder.first { d.state.isOpponentOf(it, caster) }
        d.putPermanentOnBattlefield(caster, "Leyline of the Void")
        val victim = d.putCreatureOnBattlefield(opponent, "Darksteel Myr")
        val other = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        cast(d, caster, 0)
        d.submitCardSelection(opponent, listOf(victim)).error shouldBe null
        d.state.getBattlefield().contains(victim) shouldBe false
        d.getGraveyard(opponent).contains(victim) shouldBe false
        d.state.getExile(opponent).contains(victim) shouldBe true
        d.state.getBattlefield().contains(other) shouldBe true
        d.events.filterIsInstance<PermanentsSacrificedEvent>()
            .any { it.playerId == opponent && it.permanentIds == listOf(victim) } shouldBe true
        d.events.filterIsInstance<ZoneChangeEvent>()
            .last { it.entityId == victim }.wasSacrificed shouldBe true
    }
})

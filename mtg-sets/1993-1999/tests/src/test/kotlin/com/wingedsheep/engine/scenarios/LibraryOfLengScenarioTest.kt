package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.LibraryOfLeng
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LibraryOfLengScenarioTest : FunSpec({
    val discardSpell = card("Leng Test Discard") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(LibraryOfLeng, discardSpell))
        it.initMirrorMatch(Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 32) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (passes < 32) shouldBe true
    }

    fun library(d: GameTestDriver, player: EntityId): List<EntityId> =
        d.state.getZone(ZoneKey(player, Zone.LIBRARY))

    fun castLibrary(d: GameTestDriver) {
        val me = d.activePlayer!!
        val source = d.putCardInHand(me, LibraryOfLeng.name)
        d.giveColorlessMana(me, 1)
        d.castSpell(me, source).error shouldBe null
        settle(d)
    }

    fun startDiscardEffect(d: GameTestDriver, cardToDiscard: EntityId): EntityId {
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, discardSpell.name)
        d.castSpell(me, spell).error shouldBe null
        settle(d)
        (d.state.pendingDecision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(me, listOf(cardToDiscard)).error shouldBe null
        settle(d)
        return spell
    }

    for (choice in listOf(false, true)) {
        test("effect discard may put the discarded card on top; choice=$choice") {
            val d = setup()
            val me = d.activePlayer!!
            castLibrary(d)
            val fodder = d.putCardInHand(me, "Grizzly Bears")
            startDiscardEffect(d, fodder)
            (d.state.pendingDecision is YesNoDecision) shouldBe true
            d.submitYesNo(me, choice).error shouldBe null
            settle(d)

            (fodder in d.getGraveyard(me)) shouldBe !choice
            (library(d, me).firstOrNull() == fodder) shouldBe choice
        }
    }

    test("a discard cost is not replaced") {
        val d = setup()
        val me = d.activePlayer!!
        castLibrary(d)
        val fodder = d.putCardInHand(me, "Grizzly Bears")

        d.zones.discardCards(d.state, me, listOf(fodder)).also {
            d.replaceState(it.state)
        }

        (d.state.pendingDecision == null) shouldBe true
        (fodder in d.getGraveyard(me)) shouldBe true
        (library(d, me).firstOrNull() == fodder) shouldBe false
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.Ponder
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PonderScenarioTest : FunSpec({
    for (shuffle in listOf(false, true)) {
        test("Ponder reorders three cards, optionally shuffles ($shuffle), then draws") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + Ponder)
            d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val handBefore = d.getHandSize(me)
            val first = d.putCardOnTopOfLibrary(me, "Island")
            val second = d.putCardOnTopOfLibrary(me, "Mountain")
            val third = d.putCardOnTopOfLibrary(me, "Swamp")
            val spell = d.putCardInHand(me, Ponder.name)
            d.giveMana(me, Color.BLUE, 1)
            d.castSpell(me, spell).error shouldBe null
            d.bothPass()
            d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
            d.submitOrderedResponse(me, listOf(first, second, third)).error shouldBe null
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            val shuffles = d.events.count { it is LibraryShuffledEvent }
            d.submitYesNo(me, shuffle).error shouldBe null
            d.events.count { it is LibraryShuffledEvent } shouldBe shuffles + if (shuffle) 1 else 0
            d.getHandSize(me) shouldBe handBefore + 1
            if (!shuffle) {
                d.getHand(me).contains(first) shouldBe true
                d.state.getZone(ZoneKey(me, Zone.LIBRARY)).take(2) shouldBe listOf(second, third)
            }
            d.getGraveyard(me).contains(spell) shouldBe true
        }
    }
})

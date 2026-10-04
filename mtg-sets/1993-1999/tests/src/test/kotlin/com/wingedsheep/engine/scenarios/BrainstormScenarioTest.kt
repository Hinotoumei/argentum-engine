package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.Brainstorm
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class BrainstormScenarioTest : FunSpec({
    test("Brainstorm draws three then returns an old and a newly drawn card to the library") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Brainstorm)
        d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val old = d.getHand(me).first()
        val handBefore = d.getHandSize(me)
        val drawn = d.putCardOnTopOfLibrary(me, "Island")
        d.putCardOnTopOfLibrary(me, "Mountain")
        d.putCardOnTopOfLibrary(me, "Swamp")
        val spell = d.putCardInHand(me, Brainstorm.name)
        d.giveMana(me, Color.BLUE, 1)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass()
        d.getHandSize(me) shouldBe handBefore + 3
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldContain old
        choice.options shouldContain drawn
        d.submitCardSelection(me, listOf(old, drawn)).error shouldBe null
        if (d.pendingDecision != null) {
            d.submitDecision(me, OrderedResponse(d.pendingDecision!!.id, listOf(drawn, old))).error shouldBe null
        }
        d.getHandSize(me) shouldBe handBefore + 1
        d.state.getZone(ZoneKey(me, Zone.LIBRARY)).take(2).toSet() shouldBe setOf(old, drawn)
        d.getGraveyard(me).contains(spell) shouldBe true
    }
})

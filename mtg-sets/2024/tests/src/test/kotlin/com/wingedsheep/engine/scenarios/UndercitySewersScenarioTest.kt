package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.UndercitySewers
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UndercitySewersScenarioTest : FunSpec({
    for (discard in listOf(false, true)) {
        test("Undercity Sewers enters tapped and surveils one; put in graveyard=$discard") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + UndercitySewers)
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val looked = d.putCardOnTopOfLibrary(me, "Forest")
            val land = d.putCardInHand(me, UndercitySewers.name)
            d.playLand(me, land).error shouldBe null
            d.isTapped(land) shouldBe true
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldBe listOf(looked)
            d.submitCardSelection(me, if (discard) listOf(looked) else emptyList()).error shouldBe null
            d.getGraveyard(me).contains(looked) shouldBe discard
            if (!discard) d.state.getZone(ZoneKey(me, Zone.LIBRARY)).first() shouldBe looked
        }
    }
})

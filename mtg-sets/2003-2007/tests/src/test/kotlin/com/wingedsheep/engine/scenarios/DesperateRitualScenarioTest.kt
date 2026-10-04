package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.DesperateRitual
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DesperateRitualScenarioTest : FunSpec({
    for (splice in listOf(false, true)) {
        test("Desperate Ritual pays its spell and splice costs and adds mana; splice=$splice") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + DesperateRitual)
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val spell = d.putCardInHand(me, DesperateRitual.name)
            val revealed = if (splice) d.putCardInHand(me, DesperateRitual.name) else null
            d.giveMana(me, Color.RED, if (splice) 4 else 2)
            d.submit(CastSpell(me, spell, splicedCardIds = listOfNotNull(revealed))).error shouldBe null
            d.bothPass()
            d.state.getEntity(me)!!.get<ManaPoolComponent>()!!.red shouldBe if (splice) 6 else 3
            d.getGraveyard(me).contains(spell) shouldBe true
            if (revealed != null) d.getHand(me).contains(revealed) shouldBe true
        }
    }
    test("Desperate Ritual cannot splice without paying the additional cost") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DesperateRitual)
        d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, DesperateRitual.name)
        val revealed = d.putCardInHand(me, DesperateRitual.name)
        d.giveMana(me, Color.RED, 3)
        val before = d.state
        d.submit(CastSpell(me, spell, splicedCardIds = listOf(revealed))).error shouldNotBe null
        d.state shouldBe before
    }
})

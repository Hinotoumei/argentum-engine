package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrayOgre
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain

class GrayOgreScenarioTest : FunSpec({
    test("printed Gray Ogre casts for three mana and deals two combat damage") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(GrayOgre))
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val ogre = d.putCardInHand(me, "Gray Ogre")
        d.giveMana(me, Color.RED, 3)
        d.castSpell(me, ogre).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getBattlefield() shouldContain ogre
        d.removeSummoningSickness(ogre)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(ogre), opponent)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opponent, emptyMap())
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 18
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.mtg.sets.definitions.dka.cards.FaithlessLooting
import com.wingedsheep.mtg.sets.definitions.por.cards.PhantomWarrior
import com.wingedsheep.mtg.sets.definitions.m10.cards.CentaurCourser

class FaithlessLootingScenarioTest : FunSpec({
    for (flashback in listOf(false, true)) {
        test("draw happens before discarding with flashback $flashback") {
            val d = GameTestDriver()
            d.registerCards(listOf(FaithlessLooting, PhantomWarrior, CentaurCourser, CardDefinition.basicLand("Mountain", Subtype("Mountain"))))
            d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val spell = if (flashback) d.putCardInGraveyard(me, "Faithless Looting")
                else d.putCardInHand(me, "Faithless Looting")
            val drawn1 = d.putCardOnTopOfLibrary(me, "Phantom Warrior")
            val drawn2 = d.putCardOnTopOfLibrary(me, "Centaur Courser")
            val handBefore = d.getHand(me).size
            d.giveMana(me, Color.RED, if (flashback) 3 else 1)
            d.submit(CastSpell(me, spell, useAlternativeCost = flashback,
                alternativeCostType = if (flashback) AlternativeCostType.FLASHBACK else null
            )).outcome shouldBe Outcome.Done
            d.bothPass()
            val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            choice.options shouldContain drawn1
            choice.options shouldContain drawn2
            d.getHand(me).size shouldBe handBefore + if (flashback) 2 else 1
            d.submitCardSelection(me, listOf(drawn1, drawn2))
            d.getGraveyard(me) shouldContain drawn1
            d.getGraveyard(me) shouldContain drawn2
            d.getHand(me).size shouldBe handBefore - if (flashback) 0 else 1
            if (flashback) {
                d.getExile(me) shouldContain spell
                d.getGraveyard(me) shouldNotContain spell
            } else d.getGraveyard(me) shouldContain spell
        }
    }
})

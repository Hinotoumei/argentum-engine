package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.mtg.sets.definitions.all.cards.ForceOfWill

class ForceOfWillScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ForceOfWill))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (paymentName in listOf("Phantom Warrior", "Forest", "Force of Will")) {
        test("alternative cost with $paymentName") {
            val d = setup()
            val me = d.activePlayer!!
            val creature = d.putCardInHand(me, "Centaur Courser")
            d.giveMana(me, Color.GREEN, 3)
            d.castSpell(me, creature)
            val target = d.getTopOfStack()!!
            val force = d.putCardInHand(me, "Force of Will")
            val payment = if (paymentName == "Force of Will") force else d.putCardInHand(me, paymentName)
            val before = d.state
            val result = d.submit(CastSpell(me, force, targets = listOf(ChosenTarget.Spell(target)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(lifePaid = 1, exiledCards = listOf(payment))))
            if (paymentName == "Phantom Warrior") {
                result.outcome shouldBe Outcome.Done
                d.getLifeTotal(me) shouldBe 19
                d.getExile(me).contains(payment) shouldBe true
                d.bothPass()
                d.getGraveyard(me).contains(creature) shouldBe true
            } else {
                result.outcome.shouldBeInstanceOf<Outcome.Rejected>()
                d.state shouldBe before
            }
        }
    }
})

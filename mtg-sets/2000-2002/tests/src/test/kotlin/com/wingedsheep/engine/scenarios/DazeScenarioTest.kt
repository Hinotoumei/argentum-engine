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
import com.wingedsheep.mtg.sets.definitions.nem.cards.Daze

class DazeScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(Daze))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (pay in listOf(false, true)) {
        test("returning an Island pays the alternative cost; opponent pays $pay") {
            val d = setup()
            val caster = d.activePlayer!!
            val counterCaster = d.getOpponent(caster)
            val creature = d.putCardInHand(caster, "Centaur Courser")
            d.giveMana(caster, Color.GREEN, 3)
            d.castSpell(caster, creature).outcome shouldBe Outcome.Done
            val target = d.getTopOfStack()!!
            d.passPriority(caster)
            val island = d.putLandOnBattlefield(counterCaster, "Island")
            val daze = d.putCardInHand(counterCaster, "Daze")
            d.submit(CastSpell(counterCaster, daze,
                targets = listOf(ChosenTarget.Spell(target)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(island))
            )).outcome shouldBe Outcome.Done
            d.getHand(counterCaster).contains(island) shouldBe true
            if (pay) d.giveMana(caster, Color.GREEN, 1)
            d.bothPass()
            if (d.pendingDecision != null) d.submitYesNo(caster, pay)
            if (pay) d.bothPass()
            d.state.getBattlefield().contains(creature) shouldBe pay
            d.getGraveyard(caster).contains(creature) shouldBe !pay
        }
    }
    test("a Forest cannot pay the Island alternative cost") {
        val d = setup()
        val me = d.activePlayer!!
        val creature = d.putCardInHand(me, "Centaur Courser")
        d.giveMana(me, Color.GREEN, 3)
        d.castSpell(me, creature)
        val target = d.getTopOfStack()!!
        val forest = d.putLandOnBattlefield(me, "Forest")
        val daze = d.putCardInHand(me, "Daze")
        val before = d.state
        d.submit(CastSpell(me, daze, targets = listOf(ChosenTarget.Spell(target)),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
            additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(forest))
        )).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})


package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.FatalPush
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class FatalPushScenarioTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(FatalPush))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for ((name, destroyed) in listOf("Grizzly Bears" to true, "Centaur Courser" to false,
        "Hill Giant" to false, "Hystrodon" to false)) {
        test("without revolt $name is a legal target and destruction is $destroyed") {
            val d = setup()
            val me = d.activePlayer!!
            val opponent = d.getOpponent(me)
            val creature = d.putCreatureOnBattlefield(opponent, name)
            val push = d.putCardInHand(me, "Fatal Push")
            d.giveMana(me, Color.BLACK)
            d.castSpell(me, push, listOf(creature)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.getGraveyard(opponent).contains(creature) shouldBe destroyed
            d.state.getBattlefield().contains(creature) shouldBe !destroyed
        }
    }

    for (friendly in listOf(false, true)) {
        for (name in listOf("Hill Giant", "Hystrodon")) {
            test("$name revolt evaluates after casting with friendly sacrifice $friendly") {
                val d = setup()
                val me = d.activePlayer!!
                val opponent = d.getOpponent(me)
                val creature = d.putCreatureOnBattlefield(opponent, name)
                val landOwner = if (friendly) me else opponent
                val wilds = d.putLandOnBattlefield(landOwner, "Evolving Wilds")
                val ability = d.cardRegistry.getCard("Evolving Wilds")!!.script.activatedAbilities.single()
                val push = d.putCardInHand(me, "Fatal Push")
                d.giveMana(me, Color.BLACK)
                d.castSpell(me, push, listOf(creature)).outcome shouldBe Outcome.Done
                if (!friendly) d.passPriority(me)
                d.submit(ActivateAbility(landOwner, wilds, ability.id)).outcome shouldBe Outcome.Done
                d.getGraveyard(landOwner).contains(wilds) shouldBe true
                d.bothPass()
                val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                d.submitCardSelection(landOwner, emptyList())
                d.pendingDecision shouldBe null
                d.bothPass()
                val destroyed = friendly && name == "Hill Giant"
                d.getGraveyard(opponent).contains(creature) shouldBe destroyed
                d.state.getBattlefield().contains(creature) shouldBe !destroyed
            }
        }
    }

    test("a genuinely cast face-down Hystrodon has mana value zero") {
        val d = setup()
        val me = d.activePlayer!!
        val creature = d.putCardInHand(me, "Hystrodon")
        d.giveMana(me, Color.GREEN, 3)
        d.submit(CastSpell(me, creature, castFaceDown = true)).outcome shouldBe Outcome.Done
        d.bothPass()
        val push = d.putCardInHand(me, "Fatal Push")
        d.giveMana(me, Color.BLACK)
        d.castSpell(me, push, listOf(creature)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(me).contains(creature) shouldBe true
    }

    test("a player is not a legal target and rejection spends nothing") {
        val d = setup()
        val me = d.activePlayer!!
        val push = d.putCardInHand(me, "Fatal Push")
        d.giveMana(me, Color.BLACK)
        val before = d.state
        d.submit(CastSpell(me, push, targets = listOf(ChosenTarget.Player(d.getOpponent(me)))))
            .outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

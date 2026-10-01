package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.TasteForMayhem
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TasteForMayhemScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(TasteForMayhem)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (ownHost in listOf(false, true)) {
        test("base bonus and live hellbent track the Aura controller's hand with own host $ownHost") {
            val d = setup()
            val me = d.activePlayer!!
            val opponent = d.getOpponent(me)
            val host = d.putCreatureOnBattlefield(if (ownHost) me else opponent, "Grizzly Bears")
            val aura = d.putCardInHand(me, "Taste for Mayhem")
            d.giveMana(me, Color.RED)
            d.castSpell(me, aura, listOf(host)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.state.projectedState.getPower(host) shouldBe 4
            d.state.projectedState.getToughness(host) shouldBe 2
            val emptyHand = d.putCardInHand(me, "One with Nothing")
            d.giveMana(me, Color.BLACK)
            d.castSpell(me, emptyHand).outcome shouldBe Outcome.Done
            d.bothPass()
            d.getHand(me).size shouldBe 0
            d.getHand(opponent).isNotEmpty() shouldBe true
            d.state.projectedState.getPower(host) shouldBe 6
            d.state.projectedState.getToughness(host) shouldBe 2
            val draw = d.putCardInHand(me, "Divination")
            d.giveMana(me, Color.BLUE, 3)
            d.castSpell(me, draw).outcome shouldBe Outcome.Done
            d.bothPass()
            d.getHand(me).size shouldBe 2
            d.state.projectedState.getPower(host) shouldBe 4
            d.state.projectedState.getToughness(host) shouldBe 2
            val remove = d.putCardInHand(me, "Naturalize")
            d.giveMana(me, Color.GREEN, 2)
            d.castSpell(me, remove, listOf(aura)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.state.projectedState.getPower(host) shouldBe 2
            d.state.projectedState.getToughness(host) shouldBe 2
        }
    }
    test("a player is not a legal enchanted target and rejection spends nothing") {
        val d = setup()
        val me = d.activePlayer!!
        val aura = d.putCardInHand(me, "Taste for Mayhem")
        d.giveMana(me, Color.RED)
        val before = d.state
        d.submit(CastSpell(me, aura, targets = listOf(ChosenTarget.Player(d.getOpponent(me)))))
            .outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

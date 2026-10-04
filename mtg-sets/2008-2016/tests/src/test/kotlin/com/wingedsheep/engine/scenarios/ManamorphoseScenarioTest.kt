package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.shm.cards.Manamorphose
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ManamorphoseScenarioTest : FunSpec({
    for (colors in listOf(listOf(Color.WHITE, Color.BLUE), listOf(Color.WHITE, Color.WHITE))) {
        test("Manamorphose adds the chosen two-mana combination $colors then draws") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + Manamorphose)
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val drawn = d.putCardOnTopOfLibrary(me, "Forest")
            val spell = d.putCardInHand(me, Manamorphose.name)
            d.giveMana(me, Color.RED, 2)
            d.castSpell(me, spell).error shouldBe null
            d.bothPass()
            for (color in colors) {
                val choice = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
                d.submitDecision(me, ColorChosenResponse(choice.id, color)).error shouldBe null
            }
            val pool = d.state.getEntity(me)!!.get<ManaPoolComponent>()!!
            pool.white shouldBe colors.count { it == Color.WHITE }
            pool.blue shouldBe colors.count { it == Color.BLUE }
            pool.red shouldBe 0
            d.getHand(me).contains(drawn) shouldBe true
            d.getGraveyard(me).contains(spell) shouldBe true
        }
    }
})

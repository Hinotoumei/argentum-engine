package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.mtg.sets.definitions.lea.cards.UndergroundSea
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UndergroundSeaScenarioTest : FunSpec({
    test("Underground Sea enters untapped and its two intrinsic abilities produce blue or black") {
        val outputs = (0..1).map { index ->
            val d = GameTestDriver()
            d.registerCards(TestCards.all + UndergroundSea)
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val land = d.putCardInHand(me, UndergroundSea.name)
            d.playLand(me, land).error shouldBe null
            d.isTapped(land) shouldBe false
            val abilities = d.legalActions(me).map { it.action }.filterIsInstance<ActivateAbility>()
                .filter { it.sourceId == land }
            abilities.size shouldBe 2
            d.submit(abilities[index]).error shouldBe null
            d.isTapped(land) shouldBe true
            d.state.stack.size shouldBe 0
            val pool = d.state.getEntity(me)!!.get<ManaPoolComponent>()!!
            pool.white + pool.red + pool.green + pool.colorless shouldBe 0
            pool.blue to pool.black
        }.toSet()
        outputs shouldBe setOf(1 to 0, 0 to 1)
    }
})

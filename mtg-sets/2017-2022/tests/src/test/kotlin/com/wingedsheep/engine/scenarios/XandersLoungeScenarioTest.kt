package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent

class XandersLoungeScenarioTest : ScenarioTestBase() {
    init {
        test("Xander's Lounge enters tapped through a normal land play") {
            val game = scenario().withPlayers().withCardInHand(1, "Xander's Lounge")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val id = game.findCardsInHand(1, "Xander's Lounge").single()
            game.execute(PlayLand(game.player1Id, id)).error shouldBe null
            game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
        }
        test("cycling Lounge pays three mana, discards it and draws a card") {
            val game = scenario().withPlayers().withCardInHand(1, "Xander's Lounge")
                .withLandsOnBattlefield(1, "Island", 3).withCardInLibrary(1, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Xander's Lounge").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Xander's Lounge") shouldBe true
            game.findCardsInHand(1, "Forest").size shouldBe 1
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class SireOfInsanityScenarioTest : ScenarioTestBase() {
    init {
        for (active in listOf(1, 2)) {
            test("Sire discards both hands at player $active's end step") {
                val game = scenario().withPlayers().withCardOnBattlefield(1, "Sire of Insanity")
                    .withCardInHand(1, "Forest").withCardInHand(1, "Mountain")
                    .withCardInHand(2, "Island").withActivePlayer(active)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.state.getHand(game.player1Id).size shouldBe 0
                game.state.getHand(game.player2Id).size shouldBe 0
                game.state.getGraveyard(game.player1Id).size shouldBe 2
                game.state.getGraveyard(game.player2Id).size shouldBe 1
            }
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class OneWithNothingScenarioTest : ScenarioTestBase() {
    init {
        test("casting One with Nothing discards the entire remaining hand") {
            val game = scenario().withPlayers().withCardInHand(1, "One with Nothing")
                .withCardInHand(1, "Forest").withCardInHand(1, "Mountain")
                .withCardInHand(2, "Island").withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val spell = game.findCardsInHand(1, "One with Nothing").single()
            game.execute(CastSpell(game.player1Id, spell)).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe 0
            game.state.getGraveyard(game.player1Id).size shouldBe 3
            game.state.getHand(game.player2Id).size shouldBe 1
        }
    }
}

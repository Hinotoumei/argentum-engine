package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent

class StartingTownScenarioTest : ScenarioTestBase() {
    init {
        for (turn in listOf(1, 2, 3, 4)) {
            test("Starting Town checks its controller's turn count $turn") {
                val game = scenario().withPlayers().withCardInHand(1, "Starting Town")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.state = game.state.updateEntity(game.player1Id) { it.with(PlayerTurnsTakenComponent(turn)) }
                val id = game.findCardsInHand(1, "Starting Town").single()
                game.execute(PlayLand(game.player1Id, id)).error shouldBe null
                game.state.getEntity(id)!!.has<TappedComponent>() shouldBe (turn > 3)
            }
        }
    }
}

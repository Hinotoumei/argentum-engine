package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class BloodCryptScenarioTest : ScenarioTestBase() {
    init {
        for (pay in listOf(false, true)) {
            test("Blood Crypt pays life and enters untapped only when chosen: $pay") {
                val game = scenario().withPlayers().withCardInHand(1, "Blood Crypt")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val id = game.findCardsInHand(1, "Blood Crypt").single()
                game.execute(PlayLand(game.player1Id, id)).error shouldBe null
                val choice = game.getPendingDecision()!!
                game.submitDecision(YesNoResponse(choice.id, pay)).error shouldBe null
                game.state.getBattlefield().contains(id) shouldBe true
                game.state.getEntity(id)!!.has<TappedComponent>() shouldBe !pay
                game.getLifeTotal(1) shouldBe if (pay) 18 else 20
            }
        }
    }
}

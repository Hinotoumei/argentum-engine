package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.fut.cards.KeldonMegaliths

class KeldonMegalithsScenarioTest : ScenarioTestBase() {
    init {
        for (empty in listOf(false, true)) {
            test("Megaliths damage ability requires hellbent: $empty") {
                val builder = scenario().withPlayers().withCardOnBattlefield(1, "Keldon Megaliths")
                    .withLandsOnBattlefield(1, "Mountain", 2).withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (!empty) builder.withCardInHand(1, "Forest")
                val game = builder.build()
                val id = game.findPermanent("Keldon Megaliths")!!
                val result = game.execute(ActivateAbility(game.player1Id, id, KeldonMegaliths.activatedAbilities[1].id,
                    targets = listOf(ChosenTarget.Player(game.player2Id))))
                (result.error == null) shouldBe empty
                game.resolveStack()
                game.getLifeTotal(2) shouldBe if (empty) 19 else 20
            }
        }
    }
}

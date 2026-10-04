package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tmp.cards.Wasteland
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WastelandScenarioTest : ScenarioTestBase() {
    init {
        test("Wasteland sacrifices itself and destroys the opponent's nonbasic land") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Wasteland")
                .withCardOnBattlefield(2, "Underground Sea").withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("Wasteland")!!
            val target = game.findPermanent("Underground Sea")!!
            game.execute(ActivateAbility(game.player1Id, source, Wasteland.activatedAbilities[1].id,
                targets = listOf(ChosenTarget.Permanent(target)))).error shouldBe null
            game.state.getGraveyard(game.player1Id).contains(source) shouldBe true
            game.resolveStack()
            game.state.getGraveyard(game.player2Id).contains(target) shouldBe true
        }
        test("Wasteland rejects a basic land without paying its sacrifice cost") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Wasteland")
                .withCardOnBattlefield(2, "Island").withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("Wasteland")!!
            val target = game.findPermanent("Island")!!
            val before = game.state
            game.execute(ActivateAbility(game.player1Id, source, Wasteland.activatedAbilities[1].id,
                targets = listOf(ChosenTarget.Permanent(target)))).error shouldNotBe null
            game.state shouldBe before
        }
    }
}

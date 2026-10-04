package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class GambleScenarioTest : ScenarioTestBase() {
    init {
        test("Gamble discards the searched card when it is the only card left in hand") {
            val game = scenario().withPlayers().withCardInHand(1, "Gamble")
                .withLandsOnBattlefield(1, "Mountain", 1).withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val gamble = game.findCardsInHand(1, "Gamble").single()
            val found = game.state.getLibrary(game.player1Id).first {
                game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest" }
            game.execute(CastSpell(game.player1Id, gamble)).error shouldBe null
            game.resolveStack()
            (game.selectCards(emptyList()).error != null) shouldBe true
            game.selectCards(listOf(found)).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe 0
            game.state.getGraveyard(game.player1Id).contains(found) shouldBe true
            game.state.getGraveyard(game.player1Id).contains(gamble) shouldBe true
        }
    }
}

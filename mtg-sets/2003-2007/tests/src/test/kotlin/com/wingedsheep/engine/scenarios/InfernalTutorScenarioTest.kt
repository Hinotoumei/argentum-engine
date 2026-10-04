package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class InfernalTutorScenarioTest : ScenarioTestBase() {
    init {
        test("hellbent is checked after Tutor leaves hand and permits any library card") {
            val game = scenario().withPlayers().withCardInHand(1, "Infernal Tutor")
                .withCardInLibrary(1, "Forest").withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val spell = game.findCardsInHand(1, "Infernal Tutor").single()
            val forest = game.state.getLibrary(game.player1Id).single()
            game.execute(CastSpell(game.player1Id, spell)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(forest)).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player1Id) shouldBe listOf(forest)
        }
        test("with a card in hand Tutor searches only for the revealed name") {
            val game = scenario().withPlayers().withCardInHand(1, "Infernal Tutor")
                .withCardInHand(1, "Forest").withCardInHand(1, "Island").withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain").withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val spell = game.findCardsInHand(1, "Infernal Tutor").single()
            val reveal = game.findCardsInHand(1, "Forest").single()
            val found = game.state.getLibrary(game.player1Id).first { it != reveal &&
                game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest" }
            game.execute(CastSpell(game.player1Id, spell)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(reveal)).error shouldBe null
            game.selectCards(listOf(found)).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Forest").size shouldBe 2
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }
    }
}

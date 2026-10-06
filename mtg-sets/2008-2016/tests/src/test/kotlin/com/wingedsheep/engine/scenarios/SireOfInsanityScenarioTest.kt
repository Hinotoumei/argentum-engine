package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class SireOfInsanityScenarioTest : ScenarioTestBase() {
    init {
        test("Sire discards Tamiyo returned to its opponent's hand before end step") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Sire of Insanity")
                .withCardOnBattlefield(1, "Island")
                .withCardInHand(1, "Unsummon").withCardInHand(1, "Forest")
                .withCardOnBattlefield(2, "Tamiyo, Inquisitive Student")
                .withCardInHand(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val tamiyo = game.findPermanent("Tamiyo, Inquisitive Student")!!
            game.castSpell(1, "Unsummon", targetId = tamiyo).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player2Id).contains(tamiyo) shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe 0
            game.state.getHand(game.player2Id).size shouldBe 0
            game.state.getGraveyard(game.player2Id).contains(tamiyo) shouldBe true
        }
        test("Sire discards both hands but does not discard Tamiyo tucked by Commit") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Sire of Insanity")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(1, "Commit // Memory").withCardInHand(1, "Forest")
                .withCardOnBattlefield(2, "Tamiyo, Inquisitive Student").withCardInHand(2, "Island")
                .withCardInLibrary(2, "Swamp").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val tamiyo = game.findPermanent("Tamiyo, Inquisitive Student")!!
            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Commit // Memory").single(),
                targets = listOf(ChosenTarget.Permanent(tamiyo)), faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.state.getHand(game.player2Id).contains(tamiyo) shouldBe false
            game.state.getLibrary(game.player2Id)[1] shouldBe tamiyo
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe 0
            game.state.getHand(game.player2Id).size shouldBe 0
            game.state.getLibrary(game.player2Id)[1] shouldBe tamiyo
        }
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



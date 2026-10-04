package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class PrismariCommandScenarioTest : ScenarioTestBase() {
    init {
        test("Command deals two damage and creates Treasure for independently chosen players") {
            val game = scenario().withPlayers().withCardInHand(1, "Prismari Command")
                .withLandsOnBattlefield(1, "Island", 2).withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Prismari Command").single(),
                chosenModes = listOf(0, 2),
                targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Player(game.player1Id)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
            game.state.getBattlefield().count {
                game.state.getEntity(it)?.get<CardComponent>()?.typeLine?.hasSubtype(Subtype.TREASURE) == true
            } shouldBe 1
        }
        test("Command draws and discards for the targeted player before destroying the artifact") {
            val game = scenario().withPlayers().withCardInHand(1, "Prismari Command")
                .withLandsOnBattlefield(1, "Island", 2).withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Lotus Petal").withCardInHand(2, "Forest")
                .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val artifact = game.findPermanent("Lotus Petal")!!
            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Prismari Command").single(),
                chosenModes = listOf(1, 3),
                targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(artifact)))).error shouldBe null
            game.resolveStack()
            game.getPendingDecision()!!.playerId shouldBe game.player2Id
            game.findCardsInHand(2, "Mountain").size shouldBe 2
            game.selectCards(game.findCardsInHand(2, "Mountain")).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(2, "Forest").size shouldBe 1
            game.state.getGraveyard(game.player1Id).contains(artifact) shouldBe true
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class ErebossInterventionScenarioTest : ScenarioTestBase() {
    init {
        test("losing the creature target prevents both the shrink and life gain") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Erebos's Intervention").withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Swamp", 3).withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val creature = game.findPermanent("Grizzly Bears")!!
            val targets = listOf(ChosenTarget.Permanent(creature))
            game.execute(CastSpell(game.player1Id,
                game.findCardsInHand(1, "Erebos's Intervention").single(), targets,
                xValue = 2, chosenModes = listOf(0), modeTargetsOrdered = listOf(targets))).error shouldBe null
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority().error shouldBe null
            game.castSpell(1, "Unsummon", creature).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(2, "Grizzly Bears").size shouldBe 1
            game.getLifeTotal(1) shouldBe 20
        }
        for (x in listOf(0, 2)) {
            test("creature mode uses announced X $x for both shrink and life") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Erebos's Intervention")
                    .withLandsOnBattlefield(1, "Swamp", x + 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                game.execute(CastSpell(game.player1Id,
                    game.findCardsInHand(1, "Erebos's Intervention").single(), targets,
                    xValue = x, chosenModes = listOf(0), modeTargetsOrdered = listOf(targets))).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20 + x
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe if (x == 2) 1 else 0
            }
        }
        for (count in listOf(0, 2, 3)) {
            test("graveyard mode with X one and $count targets enforces twice X") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Erebos's Intervention")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(2, "Mountain")
                    .withCardInGraveyard(2, "Island")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val targets = listOf(
                    ChosenTarget.Card(game.findCardsInGraveyard(1, "Forest").single(), game.player1Id, Zone.GRAVEYARD),
                    ChosenTarget.Card(game.findCardsInGraveyard(2, "Mountain").single(), game.player2Id, Zone.GRAVEYARD),
                    ChosenTarget.Card(game.findCardsInGraveyard(2, "Island").single(), game.player2Id, Zone.GRAVEYARD)
                ).take(count)
                val result = game.execute(CastSpell(game.player1Id,
                    game.findCardsInHand(1, "Erebos's Intervention").single(), targets,
                    xValue = 1, chosenModes = listOf(1), modeTargetsOrdered = listOf(targets)))
                if (count == 3) {
                    (result.error != null) shouldBe true
                    game.findCardsInHand(1, "Erebos's Intervention").size shouldBe 1
                } else {
                    result.error shouldBe null
                    game.resolveStack()
                    game.findCardsInGraveyard(1, "Forest").size shouldBe if (count == 0) 1 else 0
                    game.findCardsInGraveyard(2, "Mountain").size shouldBe if (count == 0) 1 else 0
                    game.findCardsInGraveyard(2, "Island").size shouldBe 1
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}

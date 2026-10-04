package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Way of the Pyromancer (Reality Fracture #254) — {1}{R} Legendary Enchantment:
 *   When it enters, empower Jace 2.
 *   Planeswalkers you control have "[+1]: Add {R}."
 */
class WayOfThePyromancerScenarioTest : ScenarioTestBase() {
    init {
        test("enters to empower Jace 2, and the granted +1 adds red mana through a loyalty activation") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Way of the Pyromancer")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Way of the Pyromancer").error shouldBe null
            game.resolveStack()

            val jace = game.findPermanents("Jace").single()
            fun loyalty() = game.state.getEntity(jace)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
            loyalty() shouldBe 2

            val granted = game.getLegalActions(1)
                .first { (it.action as? ActivateAbility)?.sourceId == jace && it.description.contains("Add {R}") }

            game.execute(granted.action).error shouldBe null
            loyalty() shouldBe 3
            game.resolveStack()

            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.total shouldBe 1
            game.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == jace } shouldBe true
        }
    }
}
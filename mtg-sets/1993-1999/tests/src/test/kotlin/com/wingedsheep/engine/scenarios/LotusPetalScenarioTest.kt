package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.mtg.sets.definitions.tmp.cards.LotusPetal
import com.wingedsheep.sdk.core.Color

class LotusPetalScenarioTest : ScenarioTestBase() {
    init {
        for (color in Color.entries) {
            test("Lotus Petal sacrifices itself for one $color mana") {
                val game = scenario().withPlayers().withCardOnBattlefield(1, "Lotus Petal")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val id = game.findPermanent("Lotus Petal")!!
                game.execute(ActivateAbility(game.player1Id, id, LotusPetal.activatedAbilities[0].id,
                    manaColorChoice = color)).error shouldBe null
                game.state.getGraveyard(game.player1Id).contains(id) shouldBe true
                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                val amount = when(color) {
                    Color.WHITE -> pool.white
                    Color.BLUE -> pool.blue
                    Color.BLACK -> pool.black
                    Color.RED -> pool.red
                    Color.GREEN -> pool.green
                }
                amount shouldBe 1
                game.state.stack.size shouldBe 0
            }
        }
    }
}

package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ForEachOpponentApnapTest : FunSpec({
    val seats = (0..3).map { EntityId("seat-$it") }
    fun visits(state: GameState, caster: EntityId): List<EntityId> {
        val visited = mutableListOf<EntityId>()
        val executor = ForEachExecutor({ s, _, context ->
            visited.add(context.controllerId)
            EffectResult.success(s)
        }, PredicateEvaluator(cardRegistry = null))
        val result = executor.execute(state,
            Effects.ForEachPlayer(Player.EachOpponent, listOf(Effects.DrawCards(1))) as ForEachEffect,
            EffectContext(sourceId = null, controllerId = caster))
        result.error shouldBe null
        return visited
    }
    test("opponent iteration rotates around the active player, not the caster or first seat") {
        val state = GameState(turnOrder = seats, activePlayerId = seats[2])
        visits(state, seats[0]) shouldBe listOf(seats[2], seats[3], seats[1])
        visits(state, seats[2]) shouldBe listOf(seats[3], seats[0], seats[1])
    }
    test("rotated opponent iteration still excludes teammates and players who have lost") {
        var state = GameState(turnOrder = seats, activePlayerId = seats[2])
        for ((index, player) in seats.withIndex()) state = state.withEntity(player,
            ComponentContainer().with(TeamComponent(if (index in listOf(0, 1)) 0 else 1)))
        visits(state, seats[0]) shouldBe listOf(seats[2], seats[3])
        state = state.updateEntity(seats[3]) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) }
        visits(state, seats[0]) shouldBe listOf(seats[2])
    }
})

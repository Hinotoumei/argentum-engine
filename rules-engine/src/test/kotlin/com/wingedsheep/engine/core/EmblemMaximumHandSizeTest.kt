package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.player.CreatePermanentEmblemExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EmblemMaximumHandSizeTest : FunSpec({
    val registry = CardRegistry()
    val cap = card("Five-card limit") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility { ability = SetMaximumHandSize(Player.You, DynamicAmounts.fixed(5)) }
    }
    registry.register(cap)
    val predicates = PredicateEvaluator(cardRegistry = registry)
    val me = EntityId.generate(); val other = EntityId.generate()
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun initial() = GameState(turnOrder = listOf(me, other), timestamp = 10)
        .withEntity(me, ComponentContainer()).withEntity(other, ComponentContainer())
    fun emblem(state: GameState, owner: EntityId, ability: StaticAbility) = CreatePermanentEmblemExecutor()
        .execute(state, CreatePermanentEmblemEffect(ownedStaticAbilities = listOf(ability), emblemDescription = ability.description),
            EffectContext(sourceId = null, controllerId = owner)).state
    fun limit(state: GameState, player: EntityId = me) = MaximumHandSize.effective(
        state, player, registry, predicates.conditions, predicates.conditions.amounts)
    fun permanent(state: GameState, timestamp: Long): GameState {
        val id = EntityId.generate()
        return state.withEntity(id, ComponentContainer()
            .with(CardComponent(cardDefinitionId = cap.name, name = cap.name, ownerId = me,
                manaCost = ManaCost.parse("{0}"), typeLine = TypeLine.parse("Enchantment")))
            .with(ControllerComponent(me)).with(BattlefieldEntryTimestampComponent(timestamp)))
            .addToZone(ZoneKey(me, Zone.BATTLEFIELD), id)
    }
    test("a later no-maximum emblem overrides an earlier permanent cap and survives serialization") {
        val state = emblem(permanent(initial(), 5), me, NoMaximumHandSize)
        state.timestamp shouldBe 11
        state.entities.values.single { it.has<EmblemSourceComponent>() }.get<EmblemSourceComponent>()?.createdAtTimestamp shouldBe 10
        limit(state) shouldBe null
        limit(state, other) shouldBe 7
        limit(json.decodeFromString<GameState>(json.encodeToString(state))) shouldBe null
    }
    test("a later permanent cap overrides an earlier no-maximum emblem") {
        val state = permanent(emblem(initial(), me, NoMaximumHandSize), 20)
        limit(state) shouldBe 5
        limit(json.decodeFromString<GameState>(json.encodeToString(state))) shouldBe 5
    }
    test("successive emblems with set and no-maximum statics follow creation order") {
        val unlimited = emblem(initial(), me, NoMaximumHandSize)
        val capped = emblem(unlimited, me, SetMaximumHandSize(Player.You, DynamicAmounts.fixed(4)))
        limit(capped) shouldBe 4
        limit(emblem(capped, me, NoMaximumHandSize)) shouldBe null
    }
    test("emblem set limits resolve their player scope relative to their own controller") {
        val state = emblem(initial(), other, SetMaximumHandSize(Player.EachOpponent, DynamicAmounts.fixed(3)))
        limit(state) shouldBe 3
        limit(state, other) shouldBe 7
    }
})

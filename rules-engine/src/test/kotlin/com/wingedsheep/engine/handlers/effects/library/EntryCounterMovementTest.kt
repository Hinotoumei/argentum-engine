package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.AnswerContinuation
import com.wingedsheep.engine.core.MoveCollectionAuraTargetContinuation
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class EntryCounterMovementTest : FunSpec({
    val registry = CardRegistry()
    val zones = ZoneTransitionService(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
    val executor = MoveCollectionExecutor(zones, registry)
    val player = EntityId.generate()
    val first = EntityId.generate()
    val second = EntityId.generate()
    fun setup(ids: List<EntityId>): GameState {
        var state = GameState(turnOrder = listOf(player)).withEntity(player, ComponentContainer())
        for (id in ids) {
            state = state.withEntity(id, ComponentContainer()
                .with(CardComponent(cardDefinitionId = "Entry Bear", name = "Entry Bear",
                    manaCost = ManaCost(emptyList()), typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                    ownerId = player))
                .with(OwnerComponent(player)))
                .addToZone(ZoneKey(player, Zone.GRAVEYARD), id)
        }
        return state
    }
    fun context(ids: List<EntityId>) = EffectContext(sourceId = null, controllerId = player,
        pipeline = PipelineState(storedCollections = mapOf("entering" to ids)))
    val effect = MoveCollectionEffect(from = "entering", destination = CardDestination.WithEntryCounters(
        CardDestination.ToZone(Zone.BATTLEFIELD), mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1, CounterType.CHARGE to 2)))

    test("counter placement emits its events before the battlefield entry event") {
        val result = executor.execute(setup(listOf(first)), effect, context(listOf(first)))
        val entered = result.events.indexOfFirst { it is ZoneChangeEvent && it.entityId == first && it.toZone == Zone.BATTLEFIELD }
        (entered >= 0) shouldBe true
        val counterIndices = result.events.withIndex().filter { it.value is CountersAddedEvent }.map { it.index }
        counterIndices.size shouldBe 2
        counterIndices.all { it < entered } shouldBe true
        result.state.getEntity(first)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 2
    }
    test("each card in a collection gets its own entry placement") {
        val result = executor.execute(setup(listOf(first, second)), effect, context(listOf(first, second)))
        for (id in listOf(first, second)) {
            result.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
            result.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(id) shouldBe true
        }
        result.events.filterIsInstance<CountersAddedEvent>().size shouldBe 4
    }
    test("a later ordinary blink clears the counters and does not repeat the modifier") {
        val entered = executor.execute(setup(listOf(first)), effect, context(listOf(first))).state
        val exiled = zones.moveToZone(entered, first, Zone.EXILE).state
        val returned = zones.moveToZone(exiled, first, Zone.BATTLEFIELD)
        (returned.state.getEntity(first)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
        returned.events.filterIsInstance<CountersAddedEvent>().size shouldBe 0
    }
    test("a saved Aura entry continuation retains movement-scoped counters") {
        val frame: AnswerContinuation = MoveCollectionAuraTargetContinuation(
            auraId = first, controllerId = player, destPlayerId = player,
            remainingAuras = listOf(second), sourceId = null, sourceName = null,
            entryCounters = mapOf(CounterType.CHARGE to 2)
        )
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        json.decodeFromString<AnswerContinuation>(json.encodeToString(frame)) shouldBe frame
    }
})

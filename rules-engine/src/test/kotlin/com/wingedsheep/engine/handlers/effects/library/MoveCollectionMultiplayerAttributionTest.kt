package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.*
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MoveCollectionMultiplayerAttributionTest : FunSpec({
    val registry = CardRegistry()
    val executor = MoveCollectionExecutor(ZoneTransitionService(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = registry)), registry)
    val first = EntityId.generate(); val second = EntityId.generate(); val caster = EntityId.generate()
    val a = EntityId.generate(); val b = EntityId.generate()
    fun state(zone: Zone, stolen: Boolean = false): GameState {
        var state = GameState(turnOrder = listOf(first, second, caster))
        for (id in listOf(first, second, caster)) state = state.withEntity(id, ComponentContainer())
        for ((id, owner) in listOf(a to first, b to second)) {
            val controller = if (stolen) if (owner == first) second else first else owner
            state = state.withEntity(id, ComponentContainer()
                .with(CardComponent(cardDefinitionId = "Card $id", name = "Card $id", manaCost = ManaCost.parse("{0}"),
                    typeLine = TypeLine.parse(if (id == a) "Artifact Creature — Food" else "Creature — Bear"), ownerId = owner))
                .with(OwnerComponent(owner)).with(ControllerComponent(controller)))
                .addToZone(ZoneKey(owner, zone), id)
        }
        return state
    }
    fun execute(state: GameState, kind: MoveType) = executor.execute(state,
        MoveCollectionEffect(from = "chosen", destination = CardDestination.ToZone(Zone.GRAVEYARD), moveType = kind),
        EffectContext(sourceId = null, controllerId = caster, pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(a, b)))))

    test("simultaneous discards reach each owner's graveyard with separate owner-attributed events and counters") {
        val result = execute(state(Zone.HAND), MoveType.Discard)
        result.outcome shouldBe Outcome.Done
        result.state.getGraveyard(first) shouldBe listOf(a)
        result.state.getGraveyard(second) shouldBe listOf(b)
        result.state.getGraveyard(caster) shouldBe emptyList()
        result.events.filterIsInstance<CardsDiscardedEvent>().associate { it.playerId to it.cardIds } shouldBe mapOf(first to listOf(a), second to listOf(b))
        result.state.getEntity(first)?.get<CardsDiscardedThisTurnComponent>()?.count shouldBe 1
        result.state.getEntity(second)?.get<CardsDiscardedThisTurnComponent>()?.count shouldBe 1
        result.state.getEntity(caster)?.has<CardsDiscardedThisTurnComponent>() shouldBe false
    }
    test("stolen permanents are sacrificed by their controllers but move to their owners' graveyards") {
        val result = execute(state(Zone.BATTLEFIELD, stolen = true), MoveType.Sacrifice)
        result.outcome shouldBe Outcome.Done
        result.state.getGraveyard(first) shouldBe listOf(a)
        result.state.getGraveyard(second) shouldBe listOf(b)
        result.events.filterIsInstance<PermanentsSacrificedEvent>().associate { it.playerId to it.permanentIds } shouldBe mapOf(second to listOf(a), first to listOf(b))
        result.events.filterIsInstance<ZoneChangeEvent>().filter { it.entityId in listOf(a, b) }.all { it.wasSacrificed } shouldBe true
        result.state.pendingSacrificeIds shouldBe emptySet()
        result.state.permanentsSacrificedThisTurn shouldBe 2
        for (id in listOf(first, second)) result.state.getEntity(id)?.get<PermanentsSacrificedThisTurnComponent>()?.count shouldBe 1
        result.state.getEntity(second)?.has<SacrificedFoodThisTurnComponent>() shouldBe true
        result.state.getEntity(second)?.has<SacrificedArtifactThisTurnComponent>() shouldBe true
        result.state.getEntity(first)?.has<SacrificedFoodThisTurnComponent>() shouldBe false
        result.state.getEntity(caster)?.has<SacrificedFoodThisTurnComponent>() shouldBe false
    }
    test("simultaneous sacrifices preserve previous per-player and table counts") {
        val initial = state(Zone.BATTLEFIELD, stolen = true).copy(permanentsSacrificedThisTurn = 5)
            .updateEntity(first) { it.with(PermanentsSacrificedThisTurnComponent(3)) }
            .updateEntity(second) { it.with(PermanentsSacrificedThisTurnComponent(7)) }
        val result = execute(initial, MoveType.Sacrifice)
        result.state.permanentsSacrificedThisTurn shouldBe 7
        result.state.getEntity(first)?.get<PermanentsSacrificedThisTurnComponent>()?.count shouldBe 4
        result.state.getEntity(second)?.get<PermanentsSacrificedThisTurnComponent>()?.count shouldBe 8
    }
    test("a control effect ending when an earlier selected source leaves does not reattribute the later simultaneous sacrifice") {
        val initial = state(Zone.BATTLEFIELD).copy(floatingEffects = listOf(
            ActiveFloatingEffect(id = EntityId.generate(),
                effect = FloatingEffectData(layer = Layer.CONTROL,
                    modification = SerializableModification.ChangeController(first), affectedEntities = setOf(b)),
                duration = Duration.WhileSourceOnBattlefield("Card $a"), sourceId = a,
                controllerId = first, timestamp = 1L)))
        initial.projectedState.getController(b) shouldBe first
        val result = execute(initial, MoveType.Sacrifice)
        result.events.filterIsInstance<PermanentsSacrificedEvent>().map { it.playerId to it.permanentIds } shouldBe
            listOf(first to listOf(a, b))
        result.state.getGraveyard(first) shouldBe listOf(a)
        result.state.getGraveyard(second) shouldBe listOf(b)
        result.state.getEntity(first)?.get<PermanentsSacrificedThisTurnComponent>()?.count shouldBe 2
        result.state.getEntity(second)?.has<PermanentsSacrificedThisTurnComponent>() shouldBe false
        result.events.filterIsInstance<ZoneChangeEvent>().filter { it.entityId in listOf(a, b) }.all { it.wasSacrificed } shouldBe true
        result.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == b }
            .lastKnown?.controllerId shouldBe first
        result.state.getEntity(first)?.get<PermanentLeftBattlefieldThisTurnComponent>()?.count shouldBe 2
        result.state.getEntity(second)?.has<PermanentLeftBattlefieldThisTurnComponent>() shouldBe false
    }
    test("zone-service batch freezes projected power and toughness before an earlier source leaves") {
        val initial = state(Zone.BATTLEFIELD).copy(floatingEffects = listOf(
            ActiveFloatingEffect(id = EntityId.generate(),
                effect = FloatingEffectData(layer = Layer.POWER_TOUGHNESS,
                    modification = SerializableModification.SetPowerToughness(5, 7), affectedEntities = setOf(b)),
                duration = Duration.WhileSourceOnBattlefield("Card $a"), sourceId = a,
                controllerId = first, timestamp = 1L)))
        initial.projectedState.getPower(b) shouldBe 5
        initial.projectedState.getToughness(b) shouldBe 7
        val result = ZoneTransitionService(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = registry))
            .moveToZoneBatch(initial, listOf(a, b), Zone.GRAVEYARD)
        val snapshot = result.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == b }.lastKnown
        snapshot?.power shouldBe 5
        snapshot?.toughness shouldBe 7
        result.state.getGraveyard(first) shouldBe listOf(a)
        result.state.getGraveyard(second) shouldBe listOf(b)
    }
})

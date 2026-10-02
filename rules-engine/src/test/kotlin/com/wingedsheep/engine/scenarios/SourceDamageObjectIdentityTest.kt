package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SourceDamageObjectIdentityTest : FunSpec({
    val source = EntityId.generate(); val recipient = EntityId.generate(); val owner = EntityId.generate(); val other = EntityId.generate()
    val history = DamageDealtToCreaturesThisTurnComponent(setOf(recipient))
    fun setup(zone: Zone): GameState {
        val state = GameState().withEntity(source, ComponentContainer())
        val placed = if (zone == Zone.STACK) state.pushToStack(source) else state.addToZone(ZoneKey(owner, zone), source)
        return placed.updateEntity(source) { it.with(history) }
    }
    for (destination in listOf(Zone.GRAVEYARD, Zone.EXILE, Zone.HAND)) test("a spell source forgets damage on its next zone visit: $destination") {
        val state = setup(Zone.STACK)
        val old = state.objectRef(source)
        val moved = state.removeFromStack(source).addToZone(ZoneKey(owner, destination), source)
        (moved.objectRef(source) != old) shouldBe true
        moved.getEntity(source)?.get<DamageDealtToCreaturesThisTurnComponent>() shouldBe null
    }
    test("a popped and restored resolving spell keeps its damage history through a saved state") {
        val state = setup(Zone.STACK)
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        val restored = json.decodeFromString<GameState>(json.encodeToString(state.popFromStack().second))
        val resumed = restored.pushToStack(source)
        resumed.objectRef(source) shouldBe state.objectRef(source)
        resumed.getEntity(source)?.get<DamageDealtToCreaturesThisTurnComponent>() shouldBe history
    }
    test("a battlefield control transfer retains the same source object and history") {
        val state = setup(Zone.BATTLEFIELD)
        val moved = state.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), source).addToZone(ZoneKey(other, Zone.BATTLEFIELD), source)
        moved.objectRef(source) shouldBe state.objectRef(source)
        moved.getEntity(source)?.get<DamageDealtToCreaturesThisTurnComponent>() shouldBe history
    }
    test("a same-library reorder does not manufacture a new source object") {
        val state = setup(Zone.LIBRARY)
        val moved = state.removeFromZone(ZoneKey(owner, Zone.LIBRARY), source).insertIntoZone(ZoneKey(owner, Zone.LIBRARY), source, 0)
        moved.objectRef(source) shouldBe state.objectRef(source)
        moved.getEntity(source)?.get<DamageDealtToCreaturesThisTurnComponent>() shouldBe history
    }
})

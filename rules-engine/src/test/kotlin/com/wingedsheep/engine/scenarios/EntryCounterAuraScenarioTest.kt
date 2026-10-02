package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class EntryCounterAuraScenarioTest : FunSpec({
    val aura = card("Entry Counter Aura") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        oracleText = "Enchant creature"
    }
    val returnAuras = card("Entry Counter Aura Return") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val auras = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You,
                    GameObjectFilter.Any.withSubtype("Aura")))
                move(auras, CardDestination.WithEntryCounters(CardDestination.ToZone(Zone.BATTLEFIELD),
                    mapOf(CounterType.CHARGE to 2)))
            }
        }
    }
    val prevention = card("Aura Entry Counter Prevention") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility { ability = CantReceiveCounters(GroupFilter(GameObjectFilter.Enchantment.youControl())) }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    for (prevented in listOf(false, true)) test("saved sequential Aura entry choices preserve counter replacements; prevented=$prevented") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(aura, returnAuras, prevention))
        d.initMirrorMatch(Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val host = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        d.putPermanentOnBattlefield(me, if(prevented) prevention.name else "Doubling Season")
        val auras = List(2) { d.putCardInGraveyard(me, aura.name) }
        val spell = d.putCardInHand(me, returnAuras.name)
        d.giveMana(me, Color.BLACK, 1)
        d.castSpell(me, spell).error shouldBe null
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 16) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        for (id in auras) {
            d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            d.submitTargetSelection(me, listOf(host)).error shouldBe null
            d.getPermanents(me).contains(id) shouldBe true
            (d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0) shouldBe
                if(prevented) 0 else 4
        }
        d.state.pendingDecision shouldBe null
    }

    test("redirected Aura entry neither places counters nor attaches to the chosen host") {
        val redirect = card("Aura Entry Redirect") {
            manaCost = "{2}"
            typeLine = "Enchantment"
            replacementEffect(RedirectZoneChange(Zone.EXILE,
                EventPattern.ZoneChangeEvent(GameObjectFilter.Enchantment,
                    from = Zone.GRAVEYARD, to = Zone.BATTLEFIELD)))
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(aura, returnAuras, redirect))
        d.initMirrorMatch(Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val host = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        d.putPermanentOnBattlefield(me, redirect.name)
        val id = d.putCardInGraveyard(me, aura.name)
        val spell = d.putCardInHand(me, returnAuras.name)
        d.giveMana(me, Color.BLACK, 1)
        d.castSpell(me, spell).error shouldBe null
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 16) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitTargetSelection(me, listOf(host)).error shouldBe null
        d.state.getZone(me, Zone.EXILE).contains(id) shouldBe true
        d.state.getEntity(id)?.get<AttachedToComponent>() shouldBe null
        (d.state.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()).contains(id) shouldBe false
        (d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0) shouldBe 0
        d.state.pendingDecision shouldBe null
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.Persist
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PersistScenarioTest : FunSpec({
    val witness = card("Entry Power Witness") {
        manaCost = "{3}"
        typeLine = "Creature — Human"
        power = 4; toughness = 4
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(DynamicAmounts.sourcePower())
        }
    }
    val legend = card("Legendary Entry Witness") {
        manaCost = "{2}"
        typeLine = "Legendary Creature — Human"
        power = 2; toughness = 2
    }
    val prevention = card("Entry Counter Prevention") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility { ability = CantReceiveCounters(GroupFilter(GameObjectFilter.Creature.youControl())) }
    }
    val redirect = card("Entry Zone Redirect") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        replacementEffect(RedirectZoneChange(Zone.EXILE,
            EventPattern.ZoneChangeEvent(GameObjectFilter.Creature, from = Zone.GRAVEYARD, to = Zone.BATTLEFIELD)))
    }
    val removeTarget = card("Graveyard Response") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            targets(TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD))
            effect = Effects.Exile(EffectTarget.ContextTarget(0), fromZone = Zone.GRAVEYARD)
        }
    }
    val copySpell = card("Reanimation Copy Response") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            targets(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(EffectTarget.ContextTarget(0))
        }
    }
    val cards = TestCards.all + listOf(Persist, witness, legend, prevention, redirect, removeTarget, copySpell)
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Swamp" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 24) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (passes < 24) shouldBe true
    }
    fun cast(d: GameTestDriver, target: EntityId): EntityId {
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Persist")
        d.giveMana(me, Color.BLACK, 2)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, me, Zone.GRAVEYARD))).error shouldBe null
        return spell
    }
    fun counters(d: GameTestDriver, target: EntityId) =
        d.state.getEntity(target)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    for (saved in listOf(false, true)) test("real casting returns a creature with its entry counter; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putCardInGraveyard(me, "Grizzly Bears")
        val spell = cast(d, target)
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        d.getPermanents(me).contains(target) shouldBe true
        d.getGraveyard(me).contains(spell) shouldBe true
        counters(d, target) shouldBe 1
        d.state.projectedState.getPower(target) shouldBe 1
        d.state.projectedState.getToughness(target) shouldBe 1
    }
    test("the actual entry trigger observes the reduced power") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putCardInGraveyard(me, witness.name)
        val life = d.getLifeTotal(me)
        cast(d, target); settle(d)
        d.getLifeTotal(me) shouldBe life + 3
    }
    test("a one toughness creature dies at the state-based-action boundary") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putCardInGraveyard(me, "Llanowar Elves")
        cast(d, target); settle(d)
        d.getGraveyard(me).contains(target) shouldBe true
        d.getPermanents(me).contains(target) shouldBe false
    }
    test("counter prevention applies during entry") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, prevention.name)
        val target = d.putCardInGraveyard(me, "Grizzly Bears")
        cast(d, target); settle(d)
        counters(d, target) shouldBe 0
        d.state.projectedState.getToughness(target) shouldBe 2
    }
    test("Doubling Season replaces the entry counter placement") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Doubling Season")
        val target = d.putCardInGraveyard(me, "Hill Giant")
        cast(d, target); settle(d)
        counters(d, target) shouldBe 2
        d.state.projectedState.getToughness(target) shouldBe 1
    }
    test("a zone replacement redirects entry without placing counters in exile") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, redirect.name)
        val target = d.putCardInGraveyard(me, "Grizzly Bears")
        cast(d, target); settle(d)
        d.getExile(me).contains(target) shouldBe true
        counters(d, target) shouldBe 0
    }
    test("exiling the target in response makes Persist resolve with no valid target") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putCardInGraveyard(me, "Grizzly Bears")
        cast(d, target)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, removeTarget.name)
        d.giveMana(me, Color.BLACK, 1)
        d.castSpellWithTargets(me, response, listOf(ChosenTarget.Card(target, me, Zone.GRAVEYARD))).error shouldBe null
        settle(d)
        d.getExile(me).contains(target) shouldBe true
        d.getPermanents(me).contains(target) shouldBe false
        counters(d, target) shouldBe 0
    }
    for (saved in listOf(false, true)) test("a copied spell independently reanimates its chosen target; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val first = d.putCardInGraveyard(me, "Grizzly Bears")
        val second = d.putCardInGraveyard(me, "Hill Giant")
        val original = cast(d, first)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, copySpell.name)
        d.giveMana(me, Color.BLUE, 1)
        d.castSpellWithTargets(me, response, listOf(ChosenTarget.Spell(original))).error shouldBe null
        settle(d)
        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitTargetSelection(me, listOf(second)).error shouldBe null
        settle(d)
        for (id in listOf(first, second)) {
            d.getPermanents(me).contains(id) shouldBe true
            counters(d, id) shouldBe 1
        }
    }
    for (kind in listOf("foreign", "legendary", "instant", "battlefield")) test("illegal $kind target rejects without paying or moving") {
        val d = setup(); val me = d.activePlayer!!
        val target = when(kind) {
            "foreign" -> d.putCardInGraveyard(d.getOpponent(me), "Grizzly Bears")
            "legendary" -> d.putCardInGraveyard(me, legend.name)
            "instant" -> d.putCardInGraveyard(me, "Counterspell")
            else -> d.putPermanentOnBattlefield(me, "Grizzly Bears")
        }
        val spell = d.putCardInHand(me, "Persist")
        d.giveMana(me, Color.BLACK, 2)
        val before = d.state
        val owner = if (kind == "foreign") d.getOpponent(me) else me
        (d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD))).error != null) shouldBe true
        d.state shouldBe before
    }
    test("unaffordable casting leaves the game unchanged") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putCardInGraveyard(me, "Grizzly Bears")
        val spell = d.putCardInHand(me, "Persist"); val before = d.state
        (d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, me, Zone.GRAVEYARD))).error != null) shouldBe true
        d.state shouldBe before
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.por.cards.Mountain208
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.lea.cards.HillGiant
import com.wingedsheep.mtg.sets.definitions.lea.cards.Counterspell
import com.wingedsheep.mtg.sets.definitions.dis.cards.Demonfire
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DemonfireScenarioTest : FunSpec({
    val shield = card("Demonfire Test Shield") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.PreventDamage(toGroup = GameObjectFilter.Creature) }
    }
    val destroy = card("Demonfire Test Destroy") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.Destroy(t) }
    }
    val exile = card("Demonfire Test Exile") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.Exile(t) }
    }
    val partial = card("Demonfire Partial Shield") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.PreventDamage(t, amount = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1)) }
    }
    val redirect = card("Demonfire Redirect") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.RedirectNextDamage(listOf(EffectTarget.Controller), t) }
    }
    val blink = card("Demonfire Blink") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val chosen = gather(com.wingedsheep.sdk.scripting.effects.CardSource.ChosenTargets)
                move(chosen, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.EXILE))
                move(chosen, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }
    val retrieve = card("Demonfire Retrieve") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            target(TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.Pipeline {
                val chosen = gather(com.wingedsheep.sdk.scripting.effects.CardSource.ChosenTargets)
                move(chosen, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.HAND))
            }
        }
    }
    val copy = card("Demonfire Copy") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { target(TargetFilter.InstantOrSorcerySpellOnStack); effect = Effects.CopyTargetSpell(EffectTarget.ContextTarget(0)) }
    }
    val sacrifice = card("Demonfire Sacrifice") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.SacrificeTarget(t, sacrificedByItsController = true) }
    }
    val draw = card("Demonfire Test Draw") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup(): GameTestDriver = GameTestDriver().also { d ->
        d.registerCards(listOf(Mountain208, GrizzlyBears, HillGiant, Counterspell, Demonfire, shield, destroy, exile, draw, partial, redirect, blink, retrieve, copy, sacrifice))
        d.initMirrorMatch(Deck.of("Mountain" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Initial fixture has empty hands; later hand changes use real spell actions.
        for (player in listOf(d.player1, d.player2)) for (id in d.getHand(player))
            d.replaceState(d.zones.moveToZone(d.state, id, Zone.GRAVEYARD).state)
    }
    fun cast(d: GameTestDriver, target: EntityId, x: Int): EntityId {
        val me = d.activePlayer!!
        val id = d.putCardInHand(me, "Demonfire")
        d.giveMana(me, Color.RED, 1)
        if (x > 0) d.giveColorlessMana(me, x)
        d.castXSpell(me, id, x, listOf(target)).error shouldBe null
        return id
    }
    fun resolve(d: GameTestDriver) {
        var n = 0
        while (d.stackSize > 0 && n++ < 20) d.bothPass()
        d.stackSize shouldBe 0
    }
    for (x in listOf(0, 2, 5)) test("actual X=$x casting damages a player by X") {
        val d = setup(); val opponent = d.player2
        cast(d, opponent, x); resolve(d)
        d.getLifeTotal(opponent) shouldBe 20 - x
    }
    test("lethal damage exiles the damaged creature") {
        val d = setup(); val creature = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, creature, 2); resolve(d)
        d.getExile(d.player2).contains(creature) shouldBe true
        d.getGraveyard(d.player2).contains(creature) shouldBe false
    }
    test("surviving damaged creature is exiled on later destruction this turn") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, creature, 1); resolve(d)
        val kill = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.getExile(d.player2).contains(creature) shouldBe true
    }
    for (empty in listOf(true, false)) test("prevention uses hand at resolution; empty=$empty") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val s = d.putCardInHand(me, shield.name); d.castSpell(me, s).error shouldBe null; resolve(d)
        if (!empty) d.putCardInHand(me, "Mountain")
        cast(d, creature, 1); resolve(d)
        (d.state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0) shouldBe if (empty) 1 else 0
        val kill = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d)
        d.getExile(d.player2).contains(creature) shouldBe empty
        d.getGraveyard(d.player2).contains(creature) shouldBe !empty
    }
    test("zero damage does not grant exile on later death") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, creature, 0); resolve(d)
        val kill = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.getGraveyard(d.player2).contains(creature) shouldBe true
    }
    for (empty in listOf(true, false)) for (destination in listOf("graveyard", "hand", "library", "exile"))
        test("counter to $destination reads current hand; empty=$empty") {
            val d = setup(); val me = d.activePlayer!!
            if (!empty) d.putCardInHand(me, "Mountain")
            val spell = cast(d, d.player2, 2)
            val counter = d.services.spellCounterer
            val result = when (destination) {
                "hand" -> counter.counterSpellToHand(d.state, spell)
                "library" -> counter.counterSpellToLibrary(d.state, spell, com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Top)
                "exile" -> counter.counterSpellToExile(d.state, spell, false, d.player2)
                else -> counter.counterSpell(d.state, spell)
            }
            result.error shouldBe null
            result.state.stack.contains(spell) shouldBe empty
        }
    test("drawing in response removes counter protection") {
        val d = setup(); val me = d.activePlayer!!
        val spell = cast(d, d.player2, 2)
        val response = d.putCardInHand(me, draw.name)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        d.castSpell(me, response).error shouldBe null; d.bothPass()
        d.getHandSize(me) shouldBe 1
        d.services.spellCounterer.counterSpell(d.state, spell).state.stack.contains(spell) shouldBe false
    }
    test("casting the last card in response enables protection") {
        val d = setup(); val me = d.activePlayer!!
        val response = d.putCardInHand(me, shield.name)
        val spell = cast(d, d.player2, 2)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        d.castSpell(me, response).error shouldBe null
        d.getHandSize(me) shouldBe 0
        d.services.spellCounterer.counterSpell(d.state, spell).state.stack.contains(spell) shouldBe true
    }
    test("a protected spell can still be exiled outright") {
        val d = setup(); val spell = cast(d, d.player2, 2)
        val result = d.services.spellCounterer.exileSpell(d.state, spell, makePlotted = false)
        result.state.stack.contains(spell) shouldBe false
        result.state.getExile(d.activePlayer!!).contains(spell) shouldBe true
    }
    test("a protected spell with its only target removed does not resolve") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val spell = cast(d, creature, 2)
        val response = d.putCardInHand(me, exile.name)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        d.castSpellWithTargets(me, response, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d)
        d.getGraveyard(me).contains(spell) shouldBe true
        d.getExile(d.player2).contains(creature) shouldBe true
    }
    test("partial prevention still marks the actual damaged creature") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val protection = d.putCardInHand(me, partial.name)
        d.castSpellWithTargets(me, protection, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.putCardInHand(me, "Mountain")
        cast(d, creature, 2); resolve(d)
        d.state.getEntity(creature)?.get<DamageComponent>()?.amount shouldBe 1
        val kill = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.getExile(d.player2).contains(creature) shouldBe true
    }
    test("redirected damage marks its actual recipient instead of the player target") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        val protection = d.putCardInHand(me, redirect.name)
        d.castSpellWithTargets(me, protection, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d)
        cast(d, me, 2); resolve(d)
        d.getLifeTotal(me) shouldBe 20
        d.getExile(me).contains(creature) shouldBe true
    }
    test("insufficient X payment rejects atomically") {
        val d = setup(); val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Demonfire"); d.giveMana(me, Color.RED, 1)
        val before = d.state
        (d.castXSpell(me, spell, 3, listOf(d.player2)).error != null) shouldBe true
        d.state shouldBe before
    }
    for (empty in listOf(true, false)) test("an actual opponent Counterspell observes Hellbent; empty=$empty") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        if (!empty) d.putCardInHand(me, "Mountain")
        val original = cast(d, opponent, 2)
        if (d.priorityPlayer != opponent) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val counter = d.putCardInHand(opponent, "Counterspell"); d.giveMana(opponent, Color.BLUE, 2)
        d.castSpellWithTargets(opponent, counter, listOf(ChosenTarget.Spell(original))).error shouldBe null
        resolve(d)
        d.getLifeTotal(opponent) shouldBe if(empty) 18 else 20
        d.getGraveyard(me).contains(original) shouldBe true
        d.getGraveyard(opponent).contains(counter) shouldBe true
    }
    for (recast in listOf(false, true)) test("blink clears exile rider and repeated cast does not reuse damage history; recast=$recast") {
        val d = setup(); val me = d.activePlayer!!
        val first = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val original = cast(d, first, 1); resolve(d)
        val reset = d.putCardInHand(me, blink.name)
        d.castSpellWithTargets(me, reset, listOf(ChosenTarget.Permanent(first))).error shouldBe null
        resolve(d)
        if (recast) {
            val retrieval = d.putCardInHand(me, retrieve.name)
            d.castSpellWithTargets(me, retrieval, listOf(ChosenTarget.Card(original, me, Zone.GRAVEYARD))).error shouldBe null
            resolve(d)
            val second = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
            d.giveMana(me, Color.RED, 1)
            d.castXSpell(me, original, 0, listOf(second)).error shouldBe null; resolve(d)
        }
        val kill = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(first))).error shouldBe null
        resolve(d)
        d.getGraveyard(d.player2).contains(first) shouldBe true
        d.getExile(d.player2).contains(first) shouldBe false
    }
    for (empty in listOf(false, true)) test("saved Demonfire stack preserves X and current-hand counter condition; empty=$empty") {
        val d = setup(); val me = d.activePlayer!!
        if (!empty) d.putCardInHand(me, "Mountain")
        val spell = cast(d, d.player2, 3)
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.services.spellCounterer.counterSpell(d.state, spell).state.stack.contains(spell) shouldBe empty
        resolve(d); d.getLifeTotal(d.player2) shouldBe 17
    }
    for (saved in listOf(false, true)) test("copied Demonfire keeps X and independently retargets; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val original = cast(d, d.player2, 2)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, copy.name)
        d.castSpellWithTargets(me, response, listOf(ChosenTarget.Spell(original))).error shouldBe null
        var n = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && n++ < 16) d.bothPass()
        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitTargetSelection(me, listOf(me)).error shouldBe null
        val copied = d.state.stack.single { it != original }
        d.services.spellCounterer.counterSpell(d.state, copied).state.stack.contains(copied) shouldBe true
        resolve(d)
        d.getLifeTotal(me) shouldBe 18; d.getLifeTotal(d.player2) shouldBe 18
    }
    test("later sacrifice also exiles a damaged survivor") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        cast(d, creature, 1); resolve(d)
        val kill = d.putCardInHand(me, sacrifice.name)
        d.castSpellWithTargets(me, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.getExile(me).contains(creature) shouldBe true
    }
    test("the exile rider expires at cleanup before a later turn's death") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        cast(d, creature, 1); resolve(d)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val actor = d.priorityPlayer!!
        val kill = d.putCardInHand(actor, destroy.name)
        d.castSpellWithTargets(actor, kill, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolve(d); d.getGraveyard(me).contains(creature) shouldBe true
        d.getExile(me).contains(creature) shouldBe false
    }
})

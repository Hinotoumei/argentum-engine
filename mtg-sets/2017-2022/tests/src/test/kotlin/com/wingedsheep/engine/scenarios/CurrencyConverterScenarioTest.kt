package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.ncc.cards.CurrencyConverter
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.definitions.por.cards.Mountain208
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CurrencyConverterScenarioTest : FunSpec({
    val creature = card("Converter Creature") { manaCost = "{0}"; typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    val discard = card("Converter Discard") { manaCost = "{0}"; typeLine = "Instant"; spell { effect = Effects.Discard(1) } }
    val blink = card("Converter Blink") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            target(TargetFilter.Artifact)
            effect = Effects.Pipeline {
                val chosen = gather(CardSource.ChosenTargets)
                move(chosen, CardDestination.ToZone(Zone.EXILE))
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }
    val remove = card("Converter Remove") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val chosen = target(TargetFilter.Artifact); effect = Effects.Move(chosen, Zone.HAND) }
    }
    val exileLoop = card("Converter Exile Loop") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            target(TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Any, zone = Zone.EXILE))
            effect = Effects.Pipeline {
                val chosen = gather(CardSource.ChosenTargets)
                move(chosen, CardDestination.ToZone(Zone.HAND))
                move(chosen, CardDestination.ToZone(Zone.EXILE))
            }
        }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(listOf(Mountain208, CurrencyConverter, PredefinedTokens.Treasure, creature, discard, blink, remove, exileLoop))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 32) d.passPriority(d.priorityPlayer!!).error shouldBe null
        (passes < 32) shouldBe true
    }
    fun save(d: GameTestDriver) = d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
    fun castConverter(d: GameTestDriver): EntityId {
        val me = d.activePlayer!!
        val source = d.putCardInHand(me, CurrencyConverter.name)
        d.giveColorlessMana(me, 1)
        d.castSpell(me, source).error shouldBe null
        settle(d)
        return source
    }
    fun discardCard(d: GameTestDriver, id: EntityId, accept: Boolean, saved: Boolean = false) {
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, discard.name)
        d.castSpell(me, spell).error shouldBe null
        settle(d)
        (d.state.pendingDecision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(me, listOf(id)).error shouldBe null
        settle(d)
        (d.state.pendingDecision is YesNoDecision) shouldBe true
        if (saved) save(d)
        d.submitYesNo(me, accept).error shouldBe null
        settle(d)
    }
    fun convert(d: GameTestDriver, source: EntityId, chosen: EntityId? = null, saved: Boolean = false) {
        val me = d.activePlayer!!
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[1].id)).error shouldBe null
        d.isTapped(source) shouldBe true
        if (saved) save(d)
        settle(d)
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(me, listOfNotNull(chosen)).error shouldBe null
            settle(d)
        }
    }
    fun named(d: GameTestDriver, name: String) = d.getPermanents(d.activePlayer!!).filter {
        d.state.getEntity(it)?.get<CardComponent>()?.name?.removeSuffix(" Token") == name
    }
    test("real casting pays one and an artifact may tap on its entry turn") {
        val d = setup(); val me = d.activePlayer!!
        val source = d.putCardInHand(me, CurrencyConverter.name)
        d.giveColorlessMana(me, 1)
        d.castSpell(me, source).error shouldBe null
        settle(d)
        (source in d.getPermanents(me)) shouldBe true
        convert(d, source)
        named(d, "Treasure").size shouldBe 0
        named(d, "Rogue").size shouldBe 0
    }
    for (accept in listOf(false, true)) for (saved in listOf(false, true)) test("discard exile is optional and survives a saved choice; accept=$accept saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        discardCard(d, id, accept, saved)
        (id in d.getExile(me)) shouldBe accept
        (id in d.getGraveyard(me)) shouldBe !accept
    }
    for (land in listOf(false, true)) for (saved in listOf(false, true)) test("returning a linked card creates the correct token; land=$land saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, if (land) "Mountain" else creature.name)
        discardCard(d, id, true)
        convert(d, source, id, saved)
        (id in d.getGraveyard(me)) shouldBe true
        (id in d.getExile(me)) shouldBe false
        named(d, "Treasure").size shouldBe if (land) 1 else 0
        val rogues = named(d, "Rogue")
        rogues.size shouldBe if (land) 0 else 1
        for (token in rogues) {
            d.state.projectedState.getPower(token) shouldBe 2
            d.state.projectedState.getToughness(token) shouldBe 2
            d.state.projectedState.hasColor(token, Color.BLACK) shouldBe true
        }
    }
    test("unrelated cards in exile are not linked and create no token") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInExile(me, creature.name)
        convert(d, source)
        (id in d.getExile(me)) shouldBe true
        named(d, "Rogue").size shouldBe 0
        named(d, "Treasure").size shouldBe 0
    }
    test("two mana and tap draws then discards and offers the new discard for exile") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        val before = d.getHandSize(me)
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[0].id)).error shouldBe null
        d.isTapped(source) shouldBe true
        settle(d)
        d.getHandSize(me) shouldBe before + 1
        (d.state.pendingDecision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(me, listOf(id)).error shouldBe null
        settle(d)
        d.submitYesNo(me, true).error shouldBe null
        settle(d)
        d.getHandSize(me) shouldBe before
        (id in d.getExile(me)) shouldBe true
    }
    test("insufficient mana for the draw ability rejects before tapping") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val before = d.state
        val result = d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[0].id))
        (result.error != null) shouldBe true
        d.state shouldBe before
        d.isTapped(source) shouldBe false
    }
    test("a tapped Converter cannot activate either ability") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        convert(d, source)
        d.giveColorlessMana(me, 2)
        for (ability in CurrencyConverter.activatedAbilities) {
            val before = d.state
            val result = d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = ability.id))
            (result.error != null) shouldBe true
            d.state shouldBe before
        }
    }
    test("blink loses this artifact visit's linked pile") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        discardCard(d, id, true)
        val response = d.putCardInHand(me, blink.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(source))).error shouldBe null
        settle(d)
        convert(d, source)
        (id in d.getExile(me)) shouldBe true
        named(d, "Rogue").size shouldBe 0
    }
    for (saved in listOf(false, true)) test("conversion already on the stack keeps its original linked pile after blink; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        discardCard(d, id, true)
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[1].id)).error shouldBe null
        val response = d.putCardInHand(me, blink.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(source))).error shouldBe null
        if (saved) save(d)
        settle(d)
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(me, listOf(id)).error shouldBe null
            settle(d)
        }
        (id in d.getGraveyard(me)) shouldBe true
        named(d, "Rogue").size shouldBe 1
        d.isTapped(source) shouldBe false
        convert(d, source)
        named(d, "Rogue").size shouldBe 1
    }
    for (saved in listOf(false, true)) test("choose one of two linked cards and retain the other; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val land = d.putCardInHand(me, "Mountain")
        discardCard(d, land, true)
        val nonland = d.putCardInHand(me, creature.name)
        discardCard(d, nonland, true)
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[1].id)).error shouldBe null
        settle(d)
        (d.state.pendingDecision is SelectCardsDecision) shouldBe true
        if (saved) save(d)
        d.submitCardSelection(me, listOf(land)).error shouldBe null
        settle(d)
        (land in d.getGraveyard(me)) shouldBe true
        (nonland in d.getExile(me)) shouldBe true
        named(d, "Treasure").size shouldBe 1
        named(d, "Rogue").size shouldBe 0
        d.untapPermanent(source)
        convert(d, source, nonland)
        (nonland in d.getGraveyard(me)) shouldBe true
        named(d, "Rogue").size shouldBe 1
        named(d, "Treasure").size shouldBe 1
    }
    for (saved in listOf(false, true)) test("conversion resolves from its departed source's linked pile; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        discardCard(d, id, true)
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = CurrencyConverter.activatedAbilities[1].id)).error shouldBe null
        val response = d.putCardInHand(me, remove.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(source))).error shouldBe null
        if (saved) save(d)
        settle(d)
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(me, listOf(id)).error shouldBe null
            settle(d)
        }
        (source in d.getHand(me)) shouldBe true
        (id in d.getGraveyard(me)) shouldBe true
        named(d, "Rogue").size shouldBe 1
    }
    for (saved in listOf(false, true)) test("a card that leaves exile and returns is no longer linked; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val source = castConverter(d)
        val id = d.putCardInHand(me, creature.name)
        discardCard(d, id, true)
        val response = d.putCardInHand(me, exileLoop.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(id, me, Zone.EXILE))).error shouldBe null
        if (saved) save(d)
        settle(d)
        (id in d.getExile(me)) shouldBe true
        convert(d, source)
        (id in d.getExile(me)) shouldBe true
        named(d, "Rogue").size shouldBe 0
    }
})

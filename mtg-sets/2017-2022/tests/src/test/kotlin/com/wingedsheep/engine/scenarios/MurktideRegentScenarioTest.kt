package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.mh2.cards.MurktideRegent
import com.wingedsheep.mtg.sets.definitions.por.cards.Mountain208
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MurktideRegentScenarioTest : FunSpec({
    val instant = card("Delve Instant") { manaCost = "{0}"; typeLine = "Instant" }
    val sorcery = card("Delve Sorcery") { manaCost = "{0}"; typeLine = "Sorcery" }
    val creature = card("Delve Creature") { manaCost = "{0}"; typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val prevention = card("Delve Counter Prevention") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = com.wingedsheep.sdk.scripting.CantReceiveCounters(com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(GameObjectFilter.Creature.youControl())) }
    }
    val doubling = card("Delve Counter Doubling") {
        manaCost = "{0}"; typeLine = "Enchantment"
        replacementEffect(com.wingedsheep.sdk.scripting.DoubleCounterPlacement())
    }
    val returnFromExile = card("Delve Exile Return") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val selected = target(TargetFilter(GameObjectFilter.Any, zone = Zone.EXILE)); effect = Effects.Move(selected, Zone.HAND, fromZone = Zone.EXILE) }
    }
    val mass = card("Delve Mass Return") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline {
            val selected = gather(com.wingedsheep.sdk.scripting.effects.CardSource.FromZone(Zone.GRAVEYARD,
                filter = GameObjectFilter.InstantOrSorcery))
            move(selected, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.HAND))
        } }
    }
    val destroy = card("Delve Destroy") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val selected = target(TargetFilter.Creature); effect = Effects.Destroy(selected) }
    }
    // Startled Awake's real graveyard-return shape, isolated from unrelated milling.
    val front = card("Delve Returning Sorcery") {
        manaCost = "{0}"; typeLine = "Sorcery"
        activatedAbility { cost = Costs.Mana("{0}"); activateFromZone = Zone.GRAVEYARD
            effect = Effects.ReturnSelfFromGraveyardTransformed() }
    }
    val returning = front.copy(backFace = card("Delve Returned Creature") {
        manaCost = ""; typeLine = "Creature — Nightmare"; power = 1; toughness = 1
    })
    val copy = card("Delve Copy") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val selected = target(TargetFilter.SpellOnStack); effect = Effects.CopyTargetSpell(selected) }
    }
    val blink = card("Delve Blink") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val selected = gather(com.wingedsheep.sdk.scripting.effects.CardSource.ChosenTargets)
                move(selected, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.EXILE))
                move(selected, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }
    val movers = listOf(Zone.HAND, Zone.LIBRARY, Zone.EXILE).map { destination ->
        card("Delve Move $destination") {
            manaCost = "{0}"; typeLine = "Instant"
            spell {
                val selected = target(TargetFilter(GameObjectFilter.Any, zone = Zone.GRAVEYARD))
                effect = Effects.Move(selected, destination, fromZone = Zone.GRAVEYARD)
            }
        }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(listOf(Mountain208, MurktideRegent, instant, sorcery, creature, copy, blink, prevention, doubling, returnFromExile, mass, destroy, returning) + movers)
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 30) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (passes < 30) shouldBe true
    }
    fun counters(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    for (saved in listOf(false, true)) for (count in listOf(0, 2, 5)) {
        test("delve counts only actual instant and sorcery payment; count=$count saved=$saved") {
            val d = setup(); val me = d.activePlayer!!
            val names = listOf("Delve Instant", "Delve Sorcery", "Delve Creature", "Mountain", "Delve Instant").take(count)
            val paid = names.map { d.putCardInGraveyard(me, it) }
            d.putCardInGraveyard(me, "Delve Sorcery") // Unpaid cards cannot contribute.
            val spell = d.putCardInHand(me, "Murktide Regent")
            d.giveMana(me, Color.BLUE, 7 - count)
            d.submit(CastSpell(playerId = me, cardId = spell, targets = emptyList(),
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
            paid.all { it in d.getExile(me) } shouldBe true
            if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            settle(d)
            (spell in d.getPermanents(me)) shouldBe true
            counters(d, spell) shouldBe names.count { it == "Delve Instant" || it == "Delve Sorcery" }
        }
    }
    test("a second delve cast triggers the first Regent once per instant or sorcery") {
        val d = setup(); val me = d.activePlayer!!
        val first = d.putPermanentOnBattlefield(me, "Murktide Regent")
        val paid = listOf("Delve Instant", "Delve Sorcery", "Delve Creature", "Mountain", "Delve Instant").map { d.putCardInGraveyard(me, it) }
        val second = d.putCardInHand(me, "Murktide Regent")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(CastSpell(playerId = me, cardId = second, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
        settle(d)
        counters(d, first) shouldBe 3
        counters(d, second) shouldBe 3
    }
    for (destination in listOf(Zone.HAND, Zone.LIBRARY, Zone.EXILE)) {
        for (owned in listOf(true, false)) test("graveyard departure to $destination uses ownership; own=$owned") {
            val d = setup(); val me = d.activePlayer!!; val owner = if (owned) me else d.getOpponent(me)
            val regent = d.putPermanentOnBattlefield(me, "Murktide Regent")
            val leaving = d.putCardInGraveyard(owner, "Delve Instant")
            val spell = d.putCardInHand(me, "Delve Move $destination")
            d.castSpellWithTargets(me, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(leaving, owner, Zone.GRAVEYARD))).error shouldBe null
            settle(d)
            counters(d, regent) shouldBe if (owned) 1 else 0
            (leaving in d.getGraveyard(owner)) shouldBe false
        }
    }
    test("creature cards leaving your graveyard do not trigger") {
        val d = setup(); val me = d.activePlayer!!
        val regent = d.putPermanentOnBattlefield(me, "Murktide Regent")
        val leaving = d.putCardInGraveyard(me, "Delve Creature")
        val spell = d.putCardInHand(me, "Delve Move EXILE")
        d.castSpellWithTargets(me, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(leaving, me, Zone.GRAVEYARD))).error shouldBe null
        settle(d)
        counters(d, regent) shouldBe 0
    }
    for (kind in listOf("too many", "duplicate", "opponent", "hand", "missing blue")) test("illegal delve is atomic: $kind") {
        val d = setup(); val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Murktide Regent")
        val legal = d.putCardInGraveyard(me, "Delve Instant")
        val paid = when (kind) {
            "too many" -> List(6) { d.putCardInGraveyard(me, "Delve Instant") }
            "duplicate" -> listOf(legal, legal)
            "opponent" -> listOf(d.putCardInGraveyard(d.getOpponent(me), "Delve Instant"))
            "hand" -> listOf(d.putCardInHand(me, "Delve Instant"))
            else -> listOf(legal)
        }
        if (kind != "missing blue") d.giveMana(me, Color.BLUE, 7)
        val before = d.state
        val result = d.submit(CastSpell(playerId = me, cardId = spell, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid)))
        (result.error != null) shouldBe true
        d.state shouldBe before
    }
    for (saved in listOf(false, true)) test("a copied permanent spell inherits delve cost objects; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val paid = List(5) { d.putCardInGraveyard(me, "Delve Instant") }
        val original = d.putCardInHand(me, "Murktide Regent")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(CastSpell(playerId = me, cardId = original, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, copy.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(original))).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        val regents = d.getPermanents(me).filter { d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Murktide Regent" }
        regents.size shouldBe 2
        regents.all { counters(d, it) == 5 } shouldBe true
    }
    for (saved in listOf(false, true)) test("blink creates a fresh Regent without delve entry counters; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val paid = List(5) { d.putCardInGraveyard(me, "Delve Instant") }
        val regent = d.putCardInHand(me, "Murktide Regent")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(CastSpell(playerId = me, cardId = regent, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
        settle(d)
        counters(d, regent) shouldBe 5
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, blink.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(regent))).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        (regent in d.getPermanents(me)) shouldBe true
        counters(d, regent) shouldBe 0
    }
    for (prevented in listOf(false, true)) test("entry and trigger counters respect replacements; prevented=$prevented") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, if (prevented) prevention.name else doubling.name)
        val paid = List(5) { d.putCardInGraveyard(me, "Delve Instant") }
        val regent = d.putCardInHand(me, "Murktide Regent")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(CastSpell(playerId = me, cardId = regent, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
        settle(d)
        counters(d, regent) shouldBe if (prevented) 0 else 10
        d.state.projectedState.getPower(regent) shouldBe if (prevented) 3 else 13
        val leaving = d.putCardInGraveyard(me, "Delve Sorcery")
        val response = d.putCardInHand(me, "Delve Move HAND")
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(leaving, me, Zone.GRAVEYARD))).error shouldBe null
        settle(d)
        counters(d, regent) shouldBe if (prevented) 0 else 12
    }
    for (saved in listOf(false, true)) test("simultaneous graveyard departures generate separate counters; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val regent = d.putPermanentOnBattlefield(me, "Murktide Regent")
        d.putCardInGraveyard(me, "Delve Instant"); d.putCardInGraveyard(me, "Delve Sorcery")
        val untouched = d.putCardInGraveyard(me, "Delve Creature")
        val response = d.putCardInHand(me, mass.name)
        d.castSpell(me, response).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        counters(d, regent) shouldBe 2
        (untouched in d.getGraveyard(me)) shouldBe true
    }
    for (saved in listOf(false, true)) test("moving a paid card while Regent waits cannot change its entry count; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val paid = List(5) { d.putCardInGraveyard(me, "Delve Instant") }
        val regent = d.putCardInHand(me, "Murktide Regent")
        d.giveMana(me, Color.BLUE, 2)
        d.submit(CastSpell(playerId = me, cardId = regent, targets = emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(delvedCards = paid))).error shouldBe null
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, returnFromExile.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(paid.first(), me, Zone.EXILE))).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        (paid.first() in d.getHand(me)) shouldBe true
        counters(d, regent) shouldBe 5
    }
    for (saved in listOf(false, true)) test("a sorcery returning transformed triggers from its graveyard characteristics; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val regent = d.putPermanentOnBattlefield(me, "Murktide Regent")
        val returned = d.putCardInGraveyard(me, returning.name)
        d.submit(ActivateAbility(playerId = me, sourceId = returned, abilityId = returning.activatedAbilities.first().id)).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        (returned in d.getPermanents(me)) shouldBe true
        d.state.getEntity(returned)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name shouldBe "Delve Returned Creature"
        counters(d, regent) shouldBe 1
    }
    test("removing Regent before its trigger resolves cannot put counters on its graveyard card") {
        val d = setup(); val me = d.activePlayer!!
        val regent = d.putPermanentOnBattlefield(me, "Murktide Regent")
        val leaving = d.putCardInGraveyard(me, "Delve Instant")
        val move = d.putCardInHand(me, "Delve Move HAND")
        d.castSpellWithTargets(me, move, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(leaving, me, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass().error shouldBe null
        d.stackSize shouldBe 1
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val response = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, response, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(regent))).error shouldBe null
        settle(d)
        (regent in d.getGraveyard(me)) shouldBe true
        counters(d, regent) shouldBe 0
    }
})

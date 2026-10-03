package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.mh2.cards.DauthiVoidwalker
import com.wingedsheep.mtg.sets.definitions.por.cards.Mountain208
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DauthiVoidwalkerScenarioTest : FunSpec({
    val void = CounterType.of("void")
    val creature = card("Void Creature") { manaCost = "{0}"; typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    val destroy = card("Void Destroy") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val chosen = target(TargetFilter.Creature); effect = Effects.Destroy(chosen) }
    }
    val draw = card("Void Draw") { manaCost = "{4}{R}"; typeLine = "Instant"; spell { effect = Effects.DrawCards(1) } }
    val xDraw = card("Void X Draw") { manaCost = "{X}{R}"; typeLine = "Instant"; spell { effect = Effects.DrawCards(1) } }
    val sorcery = card("Void Sorcery") { manaCost = "{4}{R}"; typeLine = "Sorcery"; spell { effect = Effects.DrawCards(1) } }
    val borrow = card("Void Borrow") { manaCost = "{0}"; typeLine = "Instant"; spell { val t = target(TargetFilter.Creature); effect = Effects.GainControl(t) } }
    val give = card("Void Give") { manaCost = "{0}"; typeLine = "Instant"; spell { val t = target(TargetFilter.Creature); effect = Effects.GiveControl(t, com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.AnOpponent)) } }
    val shadow = card("Void Shadow") { manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 3; toughness = 3; keywords(Keyword.SHADOW) }
    val discard = card("Void Discard") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Discard(1, com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.EachOpponent)) }
    }
    val token = card("Void Token") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.CreateToken(3, 3, creatureTypes = setOf("Bear"), controller = com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.EachOpponent)) }
    }
    val mill = card("Void Mill") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1), com.wingedsheep.sdk.scripting.references.Player.EachOpponent) } }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(listOf(Mountain208, DauthiVoidwalker, creature, destroy, draw, mill, xDraw, shadow, discard, token, sorcery, borrow, give))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 32) d.passPriority(d.priorityPlayer!!).error shouldBe null
        (passes < 32) shouldBe true
    }
    fun save(d: GameTestDriver) = d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
    fun castVoidwalker(d: GameTestDriver): EntityId {
        val me = d.activePlayer!!
        val source = d.putCardInHand(me, DauthiVoidwalker.name)
        d.giveMana(me, Color.BLACK, 2)
        d.castSpell(me, source).error shouldBe null
        settle(d)
        return source
    }
    fun counters(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)?.get<CountersComponent>()?.getCount(void) ?: 0
    fun markedCard(d: GameTestDriver, owner: EntityId, name: String): EntityId {
        val id = d.putCardInExile(owner, name)
        d.replaceState(d.state.updateEntity(id) { it.with(CountersComponent().withAdded(void, 1)) })
        return id
    }
    fun activate(d: GameTestDriver, source: EntityId, chosen: EntityId?, saved: Boolean = false) {
        val me = d.getController(source)!!
        d.removeSummoningSickness(source)
        d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = DauthiVoidwalker.activatedAbilities[0].id)).error shouldBe null
        (source in d.getGraveyard(me)) shouldBe true
        if (saved) save(d)
        settle(d)
        if (d.state.pendingDecision is SelectCardsDecision) {
            d.submitCardSelection(me, listOfNotNull(chosen)).error shouldBe null
            settle(d)
        }
    }
    for (saved in listOf(false, true)) test("opponent creature death is replaced by exile with one void counter; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        castVoidwalker(d)
        val victim = d.putPermanentOnBattlefield(opp, creature.name)
        val spell = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        if (saved) save(d)
        settle(d)
        (victim in d.getExile(opp)) shouldBe true
        (victim in d.getGraveyard(opp)) shouldBe false
        counters(d, victim) shouldBe 1
        d.events.filterIsInstance<CountersAddedEvent>().count { it.entityId == victim && it.counterType == void && it.amount == 1 } shouldBe 1
        (spell in d.getGraveyard(me)) shouldBe true
    }
    test("own creature still goes to its owner's graveyard without a void counter") {
        val d = setup(); val me = d.activePlayer!!
        castVoidwalker(d)
        val victim = d.putPermanentOnBattlefield(me, creature.name)
        val spell = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        settle(d)
        (victim in d.getGraveyard(me)) shouldBe true
        counters(d, victim) shouldBe 0
    }
    for (saved in listOf(false, true)) test("library cards are also redirected with void counters; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        castVoidwalker(d)
        val spell = d.putCardInHand(me, mill.name)
        d.castSpell(me, spell).error shouldBe null
        if (saved) save(d)
        settle(d)
        d.getGraveyard(opp).size shouldBe 0
        d.getExile(opp).size shouldBe 1
        counters(d, d.getExile(opp).single()) shouldBe 1
    }
    for (saved in listOf(false, true)) test("may cast a card marked by another source for free after sacrificing Voidwalker; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val chosen = markedCard(d, opp, draw.name)
        val before = d.getHandSize(me)
        activate(d, source, chosen, saved)
        (chosen in d.getExile(opp)) shouldBe true
        d.castSpell(me, chosen).error shouldBe null
        settle(d)
        d.getHandSize(me) shouldBe before + 1
        (chosen in d.getGraveyard(opp)) shouldBe true
    }
    for (saved in listOf(false, true)) test("may play the chosen opponent-owned land using a normal land play; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val chosen = markedCard(d, opp, "Mountain")
        activate(d, source, chosen, saved)
        d.playLand(me, chosen).error shouldBe null
        (chosen in d.getPermanents(me)) shouldBe true
        val second = d.putCardInHand(me, "Mountain")
        (d.playLand(me, second).error != null) shouldBe true
    }
    test("summoning sickness rejects activation before sacrifice") {
        val d = setup(); val me = d.activePlayer!!; val source = castVoidwalker(d)
        val before = d.state
        val result = d.submit(ActivateAbility(playerId = me, sourceId = source, abilityId = DauthiVoidwalker.activatedAbilities[0].id))
        (result.error != null) shouldBe true
        d.state shouldBe before
        (source in d.getPermanents(me)) shouldBe true
    }
    test("activation with no eligible card still pays sacrifice and grants no free play") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val own = markedCard(d, me, draw.name)
        val unmarked = d.putCardInExile(opp, draw.name)
        activate(d, source, null)
        (d.castSpell(me, own).error != null) shouldBe true
        (d.castSpell(me, unmarked).error != null) shouldBe true
        (own in d.getExile(me)) shouldBe true
        (unmarked in d.getExile(opp)) shouldBe true
    }
    for (saved in listOf(false, true)) test("discard is still emitted when the card goes directly to exile; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        castVoidwalker(d)
        val chosen = d.putCardInHand(opp, creature.name)
        val spell = d.putCardInHand(me, discard.name)
        d.castSpell(me, spell).error shouldBe null
        settle(d)
        (d.state.pendingDecision is SelectCardsDecision) shouldBe true
        if (saved) save(d)
        d.submitCardSelection(opp, listOf(chosen)).error shouldBe null
        settle(d)
        (chosen in d.getExile(opp)) shouldBe true
        counters(d, chosen) shouldBe 1
        d.events.filterIsInstance<CardsDiscardedEvent>().count { chosen in it.cardIds } shouldBe 1
    }
    test("opponent creature tokens still die rather than receiving a void counter in exile") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        castVoidwalker(d)
        val create = d.putCardInHand(me, token.name)
        d.castSpell(me, create).error shouldBe null
        settle(d)
        val victim = d.getPermanents(opp).single()
        val spell = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        settle(d)
        (victim in d.getExile(opp)) shouldBe false
        d.events.filterIsInstance<ZoneChangeEvent>().any { it.entityId == victim && it.fromZone == Zone.BATTLEFIELD && it.toZone == Zone.GRAVEYARD } shouldBe true
        d.events.filterIsInstance<CountersAddedEvent>().any { it.entityId == victim } shouldBe false
    }
    for (hasShadow in listOf(false, true)) test("actual shadow combat restricts blockers; blocker shadow=$hasShadow") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val attacker = castVoidwalker(d)
        val blocker = d.putPermanentOnBattlefield(opp, if (hasShadow) shadow.name else creature.name)
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val before = d.state
        val result = d.declareBlockers(opp, mapOf(blocker to listOf(attacker)))
        (result.error == null) shouldBe hasShadow
        if (!hasShadow) { d.state shouldBe before; d.declareNoBlockers(opp).error shouldBe null }
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        if (hasShadow) {
            (attacker in d.getGraveyard(me)) shouldBe true
            (blocker in d.getExile(opp)) shouldBe true
            counters(d, blocker) shouldBe 1
        } else d.getLifeTotal(opp) shouldBe 17
    }
    for (x in listOf(0, 3)) test("free casting permits only X zero; X=$x") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val chosen = markedCard(d, opp, xDraw.name)
        activate(d, source, chosen)
        val before = d.state
        val result = d.submit(CastSpell(playerId = me, cardId = chosen, xValue = x, paymentStrategy = PaymentStrategy.AutoPay))
        (result.error == null) shouldBe (x == 0)
        if (x == 0) { settle(d); (chosen in d.getGraveyard(opp)) shouldBe true }
        else d.state shouldBe before
    }
    for (opponentOwns in listOf(false, true)) test("the destination owner's graveyard controls replacement after control changes; opponent owns=$opponentOwns") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        castVoidwalker(d)
        val owner = if (opponentOwns) opp else me
        val victim = d.putPermanentOnBattlefield(owner, creature.name)
        val control = d.putCardInHand(me, if (opponentOwns) borrow.name else give.name)
        d.castSpellWithTargets(me, control, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        settle(d)
        d.state.projectedState.getController(victim) shouldBe if (opponentOwns) me else opp
        val spell = d.putCardInHand(me, destroy.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        settle(d)
        (victim in d.getExile(owner)) shouldBe opponentOwns
        (victim in d.getGraveyard(owner)) shouldBe !opponentOwns
        counters(d, victim) shouldBe if (opponentOwns) 1 else 0
    }
    test("choosing one of two void-marked cards grants only the selected card") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val chosen = markedCard(d, opp, draw.name)
        val other = markedCard(d, opp, draw.name)
        activate(d, source, chosen, true)
        val before = d.state
        (d.castSpell(me, other).error != null) shouldBe true
        d.state shouldBe before
        d.castSpell(me, chosen).error shouldBe null
        settle(d)
        (other in d.getExile(opp)) shouldBe true
    }
    test("unused play permission expires at the end of the activation turn") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        val chosen = markedCard(d, opp, draw.name)
        activate(d, source, chosen)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe opp
        d.passPriority(opp).error shouldBe null
        d.priorityPlayer shouldBe me
        val before = d.state
        (d.castSpell(me, chosen).error != null) shouldBe true
        d.state shouldBe before
        (chosen in d.getExile(opp)) shouldBe true
    }
    test("free play does not permit a sorcery during the opponent's turn") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.state.getOpponents(me).first()
        val source = castVoidwalker(d)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriority(opp).error shouldBe null
        val chosen = markedCard(d, opp, sorcery.name)
        activate(d, source, chosen)
        if (d.priorityPlayer == opp) d.passPriority(opp).error shouldBe null
        d.priorityPlayer shouldBe me
        val before = d.state
        (d.castSpell(me, chosen).error != null) shouldBe true
        d.state shouldBe before
        (chosen in d.getExile(opp)) shouldBe true
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.mtg.sets.definitions.m3c.cards.Barrowgoyf
import com.wingedsheep.mtg.sets.definitions.gpt.cards.LeylineOfTheVoid
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BarrowgoyfScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(Barrowgoyf, LeylineOfTheVoid)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun attack(d: GameTestDriver, me: EntityId, goyf: EntityId) {
        d.removeSummoningSickness(goyf)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(goyf), d.getOpponent(me)).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.getOpponent(me)).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage()
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }
    test("both suspended choices resume after serializing the entire game") {
        val d = setup(); val me = d.activePlayer!!
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        val top = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        attack(d, me, goyf)
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitYesNo(me, true).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options.contains(top) shouldBe true
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitCardSelection(me, listOf(top)).error shouldBe null
        d.getHand(me).contains(top) shouldBe true
        d.pendingDecision shouldBe null
    }
    test("normal casting pays printed cost and has live zero-type stats") {
        val d = setup(); val me = d.activePlayer!!
        val goyf = d.putCardInHand(me, "Barrowgoyf")
        d.giveMana(me, Color.BLACK, 3)
        d.castSpell(me, goyf).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getPower(goyf) shouldBe 0
        d.state.projectedState.getToughness(goyf) shouldBe 1
    }
    test("two mana cannot cast the three-mana spell") {
        val d = setup(); val me = d.activePlayer!!
        val goyf = d.putCardInHand(me, "Barrowgoyf")
        d.giveMana(me, Color.BLACK, 2)
        val before = d.state
        d.castSpell(me, goyf).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("graveyard card types are deduplicated across both players") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        d.putCardInGraveyard(opponent, "Mountain")
        d.putCardInGraveyard(opponent, "Grizzly Bears")
        d.state.projectedState.getPower(goyf) shouldBe 2
        d.state.projectedState.getToughness(goyf) shouldBe 3
    }
    test("declining the outer may leaves library and hand unchanged while lifelink applies") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        val library = d.state.getLibrary(me).toList(); val hand = d.getHand(me).toList()
        attack(d, me, goyf)
        d.getLifeTotal(me) shouldBe 21
        d.getLifeTotal(opponent) shouldBe 19
        d.submitYesNo(me, false).error shouldBe null
        d.state.getLibrary(me) shouldBe library
        d.getHand(me) shouldBe hand
        d.pendingDecision shouldBe null
    }
    for (take in listOf(false, true)) test("accepted mill independently permits creature return $take") {
        val d = setup(); val me = d.activePlayer!!
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        val unrelated = d.putCardInGraveyard(me, "Grizzly Bears")
        val topLand = d.putCardOnTopOfLibrary(me, "Mountain")
        val topCreature = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val librarySize = d.state.getLibrary(me).size
        val handSize = d.getHand(me).size
        attack(d, me, goyf)
        d.submitYesNo(me, true).error shouldBe null
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.contains(topCreature) shouldBe true
        decision.options.contains(unrelated) shouldBe false
        d.submitCardSelection(me, if (take) listOf(topCreature) else emptyList()).error shouldBe null
        d.getHand(me).size shouldBe handSize + (if (take) 1 else 0)
        d.state.getLibrary(me).size shouldBe librarySize - 2
        d.getGraveyard(me).contains(topLand) shouldBe true
        d.getGraveyard(me).contains(topCreature) shouldBe !take
    }
    for (size in listOf(0, 1)) test("mill handles a library of $size cards without losing to a failed draw") {
        val d = setup(); val me = d.activePlayer!!
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        d.putCardInGraveyard(me, "Grizzly Bears")
        var state = d.state
        for (id in state.getLibrary(me).toList()) state = state.removeFromZone(ZoneKey(me, Zone.LIBRARY), id)
        d.replaceState(state)
        if (size == 1) d.putCardOnTopOfLibrary(me, "Mountain")
        attack(d, me, goyf)
        d.submitYesNo(me, true).error shouldBe null
        if (d.pendingDecision is SelectCardsDecision) d.submitCardSelection(me, emptyList()).error shouldBe null
        d.state.getLibrary(me).size shouldBe 0
        d.pendingDecision shouldBe null
        d.getLifeTotal(me) shouldBe 22
    }
    test("noncombat damage to the player does not cause Barrowgoyf to mill") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Barrowgoyf")
        val library = d.state.getLibrary(me).toList()
        val shock = d.putCardInHand(me, "Shock"); d.giveMana(me, Color.RED)
        d.castSpell(me, shock, listOf(opponent)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 18
        d.state.getLibrary(me) shouldBe library
        d.pendingDecision shouldBe null
        d.state.stack.size shouldBe 0
    }
    test("deathtouch kills a larger blocker and creature combat damage does not trigger milling") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val library = d.state.getLibrary(me).toList()
        d.removeSummoningSickness(goyf)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(goyf), opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opponent, mapOf(giant to listOf(goyf))).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage()
        d.getGraveyard(opponent).contains(giant) shouldBe true
        d.getGraveyard(me).contains(goyf) shouldBe true
        d.getLifeTotal(me) shouldBe 21
        d.state.getLibrary(me) shouldBe library
        d.state.stack.size shouldBe 0
        d.pendingDecision shouldBe null
    }
    test("the combat trigger survives exiling its source in response") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(me, "Mountain")
        val top = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.removeSummoningSickness(goyf)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(goyf), opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage()
        d.passPriority(me)
        val exile = d.putCardInHand(opponent, "Scour from Existence"); d.giveColorlessMana(opponent, 7)
        d.castSpell(opponent, exile, listOf(goyf)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getExile(me).contains(goyf) shouldBe true
        d.bothPass()
        d.submitYesNo(me, true).error shouldBe null
        d.submitCardSelection(me, listOf(top)).error shouldBe null
        d.getHand(me).contains(top) shouldBe true
    }
    test("a creature redirected to exile cannot be returned by the mill trigger") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val goyf = d.putCreatureOnBattlefield(me, "Barrowgoyf")
        d.putCardInGraveyard(opponent, "Mountain")
        d.putPermanentOnBattlefield(opponent, "Leyline of the Void")
        val top = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        attack(d, me, goyf)
        d.submitYesNo(me, true).error shouldBe null
        if (d.pendingDecision != null) {
            val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.contains(top) shouldBe false
            d.submitCardSelection(me, emptyList()).error shouldBe null
        }
        d.getHand(me).contains(top) shouldBe false
        d.getExile(me).contains(top) shouldBe true
    }
    test("characteristic defining stats remain live in hand graveyard library exile and stack") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putCardInGraveyard(opponent, "Mountain")
        d.putCardInGraveyard(opponent, "Shock")
        val hand = d.putCardInHand(me, "Barrowgoyf")
        val grave = d.putCardInGraveyard(me, "Barrowgoyf")
        val library = d.putCardOnTopOfLibrary(me, "Barrowgoyf")
        val exile = d.putCardInExile(me, "Barrowgoyf")
        val evaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry).amounts
        fun check(id: EntityId) {
            val context = com.wingedsheep.engine.handlers.EffectContext(sourceId = id, controllerId = me)
            evaluator.evaluate(d.state, com.wingedsheep.sdk.dsl.DynamicAmounts.powerOf(com.wingedsheep.sdk.scripting.targets.EffectTarget.Self), context) shouldBe 3
            evaluator.evaluate(d.state, com.wingedsheep.sdk.dsl.DynamicAmounts.toughnessOf(com.wingedsheep.sdk.scripting.targets.EffectTarget.Self), context) shouldBe 4
        }
        for (id in listOf(hand, grave, library, exile)) check(id)
        d.giveMana(me, Color.BLACK, 3)
        d.castSpell(me, hand).outcome shouldBe Outcome.Done
        check(hand)
        d.bothPass()
        d.state.projectedState.getPower(hand) shouldBe 3
        d.state.projectedState.getToughness(hand) shouldBe 4
    }

})

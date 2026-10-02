package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nph.cards.SurgicalExtraction
import com.wingedsheep.mtg.sets.definitions.drk.cards.TormodsCrypt
import com.wingedsheep.mtg.sets.definitions.dst.cards.DarksteelCitadel
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SurgicalExtractionScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(SurgicalExtraction, TormodsCrypt, DarksteelCitadel)
    fun setup(life: Int = 20) = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingLife = life)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, owner: EntityId, target: EntityId): SelectCardsDecision {
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Surgical Extraction")
        d.giveMana(me, Color.BLACK)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD))).outcome shouldBe Outcome.Done
        d.bothPass()
        return d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }
    for (self in listOf(false, true)) for (selection in listOf("none", "subset", "all"))
        test("searches ${if (self) "own" else "opponent's"} zones and permits $selection matching cards") {
            val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
            val owner = if (self) me else opponent
            val grave = d.putCardInGraveyard(owner, "Grizzly Bears")
            val hand = d.putCardInHand(owner, "Grizzly Bears")
            val library = d.putCardOnTopOfLibrary(owner, "Grizzly Bears")
            val other = d.putCardInHand(owner, "Hill Giant")
            val outsiders = d.putCardInGraveyard(if (self) opponent else me, "Grizzly Bears")
            val decision = cast(d, owner, grave)
            decision.playerId shouldBe me
            decision.options.toSet() shouldBe setOf(grave, hand, library)
            val chosen = when (selection) { "none" -> emptyList(); "subset" -> listOf(hand); else -> listOf(grave, hand, library) }
            d.submitCardSelection(me, chosen).error shouldBe null
            for (id in listOf(grave, hand, library)) d.getExile(owner).contains(id) shouldBe (id in chosen)
            d.getHand(owner).contains(other) shouldBe true
            d.getExile(if (self) opponent else me).contains(outsiders) shouldBe false
            d.pendingDecision shouldBe null
            d.getGraveyard(me).any { d.getCardName(it) == "Surgical Extraction" } shouldBe true
        }
    test("Phyrexian life payment costs two life without requiring mana") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Grizzly Bears")
        val spell = d.putCardInHand(me, "Surgical Extraction")
        d.submit(CastSpell(me, spell, targets = listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD)),
            paymentStrategy = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.BLACK)))).outcome shouldBe Outcome.Done
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.submitCardSelection(me, listOf(target)).error shouldBe null
        d.getExile(owner).contains(target) shouldBe true
    }
    test("one life cannot pay the Phyrexian cost and rejection is atomic") {
        val d = setup(1); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Grizzly Bears")
        val spell = d.putCardInHand(me, "Surgical Extraction")
        val before = d.state
        d.submit(CastSpell(me, spell, targets = listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD)),
            paymentStrategy = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.BLACK)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("a basic land is an illegal target") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Mountain")
        val spell = d.putCardInHand(me, "Surgical Extraction"); d.giveMana(me, Color.BLACK)
        val before = d.state
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("the search exposes nonmatching hidden cards only to the caster and shuffling hides the library again") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Grizzly Bears")
        val otherHand = d.putCardInHand(owner, "Hill Giant")
        val otherLibrary = d.putCardOnTopOfLibrary(owner, "Mountain")
        cast(d, owner, target)
        for (id in listOf(otherHand, otherLibrary)) {
            d.state.getEntity(id)?.get<RevealedToComponent>()?.playerIds?.contains(me) shouldBe true
        }
        d.submitCardSelection(me, emptyList()).error shouldBe null
        d.state.getEntity(otherLibrary)?.get<RevealedToComponent>()?.playerIds?.contains(me) shouldBe null
        d.getHand(owner).contains(otherHand) shouldBe true
    }
    test("search selection resumes after serializing the whole game") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Grizzly Bears")
        cast(d, owner, target)
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitCardSelection(me, listOf(target)).error shouldBe null
        d.getExile(owner).contains(target) shouldBe true
        d.pendingDecision shouldBe null
    }
    test("a nonbasic land is a legal target") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Darksteel Citadel")
        cast(d, owner, target)
        d.submitCardSelection(me, listOf(target)).error shouldBe null
        d.getExile(owner).contains(target) shouldBe true
    }
    test("exiling the original target in response makes the spell fizzle without searching the other zones") {
        val d = setup(); val me = d.activePlayer!!; val owner = d.getOpponent(me)
        val target = d.putCardInGraveyard(owner, "Grizzly Bears")
        val match = d.putCardInHand(owner, "Grizzly Bears")
        val library = d.state.getLibrary(owner).toList()
        val crypt = d.putPermanentOnBattlefield(owner, "Tormod's Crypt")
        val spell = d.putCardInHand(me, "Surgical Extraction"); d.giveMana(me, Color.BLACK)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(target, owner, Zone.GRAVEYARD))).outcome shouldBe Outcome.Done
        d.passPriority(me)
        d.submit(ActivateAbility(owner, crypt, TormodsCrypt.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Player(owner)))).outcome shouldBe Outcome.Done
        d.bothPass(); d.bothPass()
        d.getExile(owner).contains(target) shouldBe true
        d.getHand(owner).contains(match) shouldBe true
        d.state.getLibrary(owner) shouldBe library
        d.pendingDecision shouldBe null
    }

})

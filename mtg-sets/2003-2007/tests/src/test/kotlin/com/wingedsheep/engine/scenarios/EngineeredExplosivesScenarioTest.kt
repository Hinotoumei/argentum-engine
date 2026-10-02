package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.EngineeredExplosives
import com.wingedsheep.mtg.sets.definitions.rav.cards.DoublingSeason
import com.wingedsheep.mtg.sets.definitions.dst.cards.DarksteelIngot
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EngineeredExplosivesScenarioTest : FunSpec({
    val addCharge = card("Test Add Charge") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val recipient = target(TargetFilter.Artifact); effect = Effects.AddCounters(CounterType.CHARGE, 2, recipient) }
    }
    val investigate = card("Test Investigate") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.CreateClue() }
    }
    val cards = TestCards.all + listOf(EngineeredExplosives, DoublingSeason, DarksteelIngot, addCharge, investigate, PredefinedTokens.Clue)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun counters(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0
    data class Payment(val x: Int, val colors: List<Color>, val colorless: Int, val expected: Int)
    for (p in listOf(
        Payment(0, emptyList(), 0, 0),
        Payment(2, listOf(Color.BLACK, Color.BLACK), 0, 1),
        Payment(2, listOf(Color.BLACK, Color.BLUE), 0, 2),
        Payment(3, listOf(Color.BLACK, Color.BLUE, Color.RED), 0, 3),
        Payment(2, emptyList(), 2, 0),
        Payment(2, listOf(Color.GREEN), 1, 1)
    )) test("X=${p.x} with ${p.colors} and ${p.colorless} colorless enters with ${p.expected} charge counters") {
        val d = setup(); val me = d.activePlayer!!
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        for (color in p.colors) d.giveMana(me, color)
        if (p.colorless > 0) d.giveColorlessMana(me, p.colorless)
        d.castXSpell(me, bomb, p.x).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(bomb) shouldBe true
        counters(d, bomb) shouldBe p.expected
    }
    test("insufficient X payment rejects without removing the card or consuming mana") {
        val d = setup(); val me = d.activePlayer!!
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.giveMana(me, Color.BLACK)
        val before = d.state
        d.castXSpell(me, bomb, 2).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("sacrifice cost preserves two charge counters for destroying both players' matching permanents") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.giveMana(me, Color.BLACK); d.giveMana(me, Color.BLUE)
        d.castXSpell(me, bomb, 2).outcome shouldBe Outcome.Done; d.bothPass()
        val mine = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val ring = d.putPermanentOnBattlefield(me, "Sol Ring")
        val land = d.putPermanentOnBattlefield(opponent, "Mountain")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.getGraveyard(me).contains(bomb) shouldBe true
        d.getPermanents(me).contains(mine) shouldBe true
        d.bothPass()
        d.getGraveyard(me).contains(mine) shouldBe true
        d.getGraveyard(opponent).contains(theirs) shouldBe true
        d.getPermanents(opponent).contains(giant) shouldBe true
        d.getPermanents(opponent).contains(land) shouldBe true
        d.getPermanents(me).contains(ring) shouldBe true
    }
    test("zero counters destroys zero-value nonland permanents while leaving lands") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.castXSpell(me, bomb, 0).outcome shouldBe Outcome.Done; d.bothPass()
        val other = d.putPermanentOnBattlefield(opponent, "Engineered Explosives")
        val land = d.putPermanentOnBattlefield(opponent, "Mountain")
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(opponent).contains(other) shouldBe true
        d.getPermanents(opponent).contains(land) shouldBe true
        d.getPermanents(opponent).contains(bear) shouldBe true
    }
    test("zero counters destroys a normally created token with zero mana value") {
        val d = setup(); val me = d.activePlayer!!
        val maker = d.putCardInHand(me, "Test Investigate")
        d.castSpell(me, maker).outcome shouldBe Outcome.Done; d.bothPass()
        val clue = d.findPermanent(me, "Clue")!!
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.castXSpell(me, bomb, 0).outcome shouldBe Outcome.Done; d.bothPass()
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(clue) shouldBe false
    }
    test("additional counters placed before activation determine the destruction amount") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.giveMana(me, Color.BLACK); d.giveMana(me, Color.BLUE)
        d.castXSpell(me, bomb, 2).outcome shouldBe Outcome.Done; d.bothPass()
        val charge = d.putCardInHand(me, "Test Add Charge")
        d.castSpell(me, charge, listOf(bomb)).outcome shouldBe Outcome.Done; d.bothPass()
        counters(d, bomb) shouldBe 4
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(opponent).contains(giant) shouldBe true
        d.getPermanents(opponent).contains(bear) shouldBe true
    }
    test("unaffordable activation does not sacrifice its source") {
        val d = setup(); val me = d.activePlayer!!
        val bomb = d.putPermanentOnBattlefield(me, "Engineered Explosives")
        d.giveColorlessMana(me, 1)
        val before = d.state
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("Doubling Season doubles the charge counters placed by sunburst") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Doubling Season")
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        d.giveMana(me, Color.BLACK); d.giveMana(me, Color.BLUE)
        d.castXSpell(me, bomb, 2).outcome shouldBe Outcome.Done; d.bothPass()
        counters(d, bomb) shouldBe 4
    }
    test("a matching indestructible artifact survives the destruction") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val bomb = d.putCardInHand(me, "Engineered Explosives")
        for (color in listOf(Color.BLACK, Color.BLUE, Color.RED)) d.giveMana(me, color)
        d.castXSpell(me, bomb, 3).outcome shouldBe Outcome.Done; d.bothPass()
        val ingot = d.putPermanentOnBattlefield(opponent, "Darksteel Ingot")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, bomb, EngineeredExplosives.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(opponent).contains(ingot) shouldBe true
        d.getGraveyard(me).contains(bomb) shouldBe true
    }

})

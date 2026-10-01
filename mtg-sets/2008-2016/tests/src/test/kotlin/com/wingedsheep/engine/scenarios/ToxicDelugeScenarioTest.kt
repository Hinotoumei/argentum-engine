package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c13.cards.ToxicDeluge
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ToxicDelugeScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(ToxicDeluge)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, x: Int): ExecutionResult {
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Toxic Deluge")
        d.giveMana(me, Color.BLACK, 3)
        return d.submit(CastSpell(me, spell,
            additionalCostPayment = AdditionalCostPayment(payXLifeAmount = x)))
    }

    test("X comes from paid life, affects both players, and leaves noncreatures alone") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val own = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val stone = d.putPermanentOnBattlefield(opponent, "Mind Stone")
        cast(d, 2).outcome shouldBe Outcome.Done
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.getGraveyard(me).contains(own) shouldBe true
        d.getGraveyard(opponent).contains(other) shouldBe true
        d.state.projectedState.getPower(giant) shouldBe 1
        d.state.projectedState.getToughness(giant) shouldBe 1
        d.state.getBattlefield().contains(stone) shouldBe true
    }

    test("X zero pays no life and leaves ordinary creatures unchanged") {
        val d = setup()
        val me = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        cast(d, 0).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(me) shouldBe 20
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.getToughness(bear) shouldBe 2
    }

    test("the affected creatures are captured at resolution and recover next turn") {
        val d = setup()
        val me = d.activePlayer!!
        val early = d.putCreatureOnBattlefield(me, "Hill Giant")
        cast(d, 2).outcome shouldBe Outcome.Done
        d.bothPass()
        val late = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        // A real spell resolution refreshes projection after the test fixture's late entry.
        val shock = d.putCardInHand(me, "Shock")
        d.giveMana(me, Color.RED)
        d.castSpell(me, shock, listOf(d.getOpponent(me))).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getToughness(late) shouldBe 2
        d.state.projectedState.getToughness(early) shouldBe 1
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getToughness(early) shouldBe 3
    }

    test("zero toughness sends an indestructible creature to the graveyard") {
        val d = setup()
        val me = d.activePlayer!!
        val ooze = d.putCreatureOnBattlefield(d.getOpponent(me), "Predator Ooze")
        cast(d, 1).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(d.getOpponent(me)).contains(ooze) shouldBe true
    }

    test("Omniscience waives mana but still requires and preserves the chosen life payment") {
        val d = setup()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Omniscience")
        val bear = d.putCreatureOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        val spell = d.putCardInHand(me, "Toxic Deluge")
        d.submit(CastSpell(me, spell, useWithoutPayingManaCost = true,
            additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2))).outcome shouldBe Outcome.Done
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.getGraveyard(d.getOpponent(me)).contains(bear) shouldBe true
    }

    test("an unrelated submitted mana X cannot override the amount of life paid") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        val spell = d.putCardInHand(me, "Toxic Deluge")
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, spell, xValue = 20,
            additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2))).outcome shouldBe Outcome.Done
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.state.getBattlefield().contains(giant) shouldBe true
        d.state.projectedState.getToughness(giant) shouldBe 1
    }

    test("Naru Meha copies the paid-life reduction as a real spell without paying life again") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant")
        cast(d, 2).outcome shouldBe Outcome.Done
        val original = d.getTopOfStack()!!
        val naru = d.putCardInHand(me, "Naru Meha, Master Wizard")
        d.giveMana(me, Color.BLUE, 4)
        d.castSpell(me, naru).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submitTargetSelection(me, listOf(original)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(me) shouldBe 18
        d.state.getEntity(d.getTopOfStack()!!)!!.has<SpellOnStackComponent>() shouldBe true
        d.bothPass()
        d.state.projectedState.getToughness(giant) shouldBe 1
        d.getLifeTotal(me) shouldBe 18
        d.bothPass()
        d.getGraveyard(opponent).contains(giant) shouldBe true
        d.getLifeTotal(me) shouldBe 18
    }

    for (x in listOf(-1, 21)) {
        test("invalid life payment $x rejects before any cost is paid") {
            val d = setup()
            val me = d.activePlayer!!
            val spell = d.putCardInHand(me, "Toxic Deluge")
            d.giveMana(me, Color.BLACK, 3)
            val before = d.state
            d.submit(CastSpell(me, spell,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = x)))
                .outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe before
        }
    }
})

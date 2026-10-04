package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BilboThiefInTheNight
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Bilbo, Thief in the Night — {1}{U} Legendary Creature — Halfling Rogue (The Hobbit #33).
 *
 *  - "Spells you cast from anywhere other than your hand cost {1} less to cast."
 *  - "Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell from your
 *    graveyard. If an instant or sorcery spell cast this way would be put into your graveyard,
 *    exile it instead."
 *
 * The trigger offers one paid cast during resolution, including sorceries during combat.
 */
class BilboThiefInTheNightScenarioTest : FunSpec({

    // A plain {2} sorcery with no targets — the cost reduction and the exile rider are both easier
    // to assert without a targeting decision in the way.
    val relic = card("Bilbo Test Relic Sorcery") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell { effect = Effects.GainLife(3) }
    }

    val artifact = card("Bilbo Test Artifact") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BilboThiefInTheNight, relic, artifact))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    fun attackWithBilbo(driver: GameTestDriver, you: EntityId, chosen: EntityId? = null, mana: Int = 1, resolveSpell: Boolean = true): EntityId {
        val bilbo = driver.putCreatureOnBattlefield(you, "Bilbo, Thief in the Night")
        driver.removeSummoningSickness(bilbo)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bilbo), driver.getOpponent(you)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        if (driver.state.pendingDecision != null) {
            driver.state.step shouldBe Step.DECLARE_ATTACKERS
            if (chosen != null) driver.giveColorlessMana(you, mana)
            driver.submitCardSelection(you, listOfNotNull(chosen))
        }
        if (!resolveSpell) return bilbo
        while (driver.state.stack.isNotEmpty() && guard++ < 30) driver.bothPass()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        return bilbo
    }

    test("a graveyard spell cast off the attack trigger costs {1} less and is exiled, not re-buried") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        driver.giveColorlessMana(you, 1)
        val lifeBefore = driver.getLifeTotal(you)
        attackWithBilbo(driver, you, sorcery)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.getLifeTotal(you) shouldBe lifeBefore + 3
        // "would be put into your graveyard, exile it instead"
        driver.getExile(you).contains(sorcery) shouldBe true
        driver.getGraveyard(you).contains(sorcery) shouldBe false
    }

    test("without the attack trigger there is no permission to cast from the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        val bilbo = driver.putCreatureOnBattlefield(you, "Bilbo, Thief in the Night")
        driver.removeSummoningSickness(bilbo)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveColorlessMana(you, 5)
        driver.castSpell(you, sorcery).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(you).contains(sorcery) shouldBe true
    }

    test("the same spell cast from hand is neither discounted nor exiled") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInHand(you, "Bilbo Test Relic Sorcery")
        attackWithBilbo(driver, you)

        // One mana is NOT enough from hand — the reduction excludes the hand.
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, sorcery).outcome shouldNotBe Outcome.Done

        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, sorcery).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        // An ordinary hand cast does not receive the attack trigger’s exile rider.
        driver.getGraveyard(you).contains(sorcery) shouldBe true
        driver.getExile(you).contains(sorcery) shouldBe false
    }

    test("the resolving trigger offers only one cast and leaves no later permission") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val first = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        val second = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        driver.giveColorlessMana(you, 1)
        attackWithBilbo(driver, you, first)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.giveColorlessMana(you, 5)
        driver.castSpell(you, second).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(you).contains(second) shouldBe true
    }
    test("an unaffordable resolving offer does not cast the spell for free") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        val before = driver.getLifeTotal(you)
        attackWithBilbo(driver, you, sorcery, mana = 0)
        driver.getLifeTotal(you) shouldBe before
        driver.getGraveyard(you).contains(sorcery) shouldBe true
        driver.getExile(you).contains(sorcery) shouldBe false
        driver.giveColorlessMana(you, 5)
        driver.castSpell(you, sorcery).outcome shouldNotBe Outcome.Done
    }

    test("an artifact can be cast during the attack trigger and enters the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val cardId = driver.putCardInGraveyard(you, "Bilbo Test Artifact")
        attackWithBilbo(driver, you, cardId)
        driver.state.getBattlefield().contains(cardId) shouldBe true
        driver.getExile(you).contains(cardId) shouldBe false
    }

    for (isSorcery in listOf(false, true)) {
        test("the countered attack-trigger spell is exiled only if instant or sorcery: $isSorcery") {
            val driver = createDriver()
            val you = driver.activePlayer!!
            val opponent = driver.getOpponent(you)
            val spell = driver.putCardInGraveyard(you,
                if (isSorcery) "Bilbo Test Relic Sorcery" else "Bilbo Test Artifact")
            val counter = driver.putCardInHand(opponent, "Counterspell")
            attackWithBilbo(driver, you, spell, resolveSpell = false)
            driver.passPriority(you).outcome shouldBe Outcome.Done
            driver.giveMana(opponent, Color.BLUE, 2)
            driver.castSpellWithTargets(opponent, counter, listOf(ChosenTarget.Spell(spell))).outcome shouldBe Outcome.Done
            var guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
            driver.getExile(you).contains(spell) shouldBe isSorcery
            driver.getGraveyard(you).contains(spell) shouldBe !isSorcery
        }
    }

})

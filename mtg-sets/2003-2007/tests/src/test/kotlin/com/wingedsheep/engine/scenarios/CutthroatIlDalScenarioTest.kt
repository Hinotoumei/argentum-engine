package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fut.cards.CutthroatIlDal
import com.wingedsheep.mtg.sets.definitions.tmp.cards.DauthiEmbrace
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CutthroatIlDalScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(CutthroatIlDal)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun emptyHand(d: GameTestDriver, me: EntityId) {
        val discard = d.putCardInHand(me, "One with Nothing")
        d.giveMana(me, Color.BLACK)
        d.castSpell(me, discard).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 0
    }
    fun attack(d: GameTestDriver, me: EntityId, attacker: EntityId) {
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), d.getOpponent(me)).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
    }
    test("printed cast creates a 4-1 and shadow tracks the controller's empty hand") {
        val d = setup(); val me = d.activePlayer!!
        val cutthroat = d.putCardInHand(me, "Cutthroat il-Dal")
        d.giveMana(me, Color.BLACK, 4)
        d.castSpell(me, cutthroat).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getPower(cutthroat) shouldBe 4
        d.state.projectedState.getToughness(cutthroat) shouldBe 1
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe false
        emptyHand(d, me)
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe true
        d.putCardInHand(me, "Swamp")
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe false
    }
    for (hellbent in listOf(false, true)) {
        test("normal and shadow blockers respect hellbent $hellbent at declaration") {
            val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
            val cutthroat = d.putCreatureOnBattlefield(me, "Cutthroat il-Dal")
            val normal = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
            val shadow = d.putCreatureOnBattlefield(opponent, "Soltari Foot Soldier")
            if (hellbent) emptyHand(d, me)
            attack(d, me, cutthroat)
            val invalid = if (hellbent) normal else shadow
            val valid = if (hellbent) shadow else normal
            val before = d.state
            d.declareBlockers(opponent, mapOf(invalid to listOf(cutthroat))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe before
            d.declareBlockers(opponent, mapOf(valid to listOf(cutthroat))).outcome shouldBe Outcome.Done
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            d.getLifeTotal(opponent) shouldBe 20
            d.getGraveyard(me).contains(cutthroat) shouldBe true
            d.getGraveyard(opponent).contains(valid) shouldBe true
        }
    }
    test("gaining shadow after a normal block does not undo that block") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val cutthroat = d.putCreatureOnBattlefield(me, "Cutthroat il-Dal")
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        attack(d, me, cutthroat)
        d.declareBlockers(opponent, mapOf(bear to listOf(cutthroat))).outcome shouldBe Outcome.Done
        d.passPriority(opponent).outcome shouldBe Outcome.Done
        d.state.step shouldBe Step.DECLARE_BLOCKERS
        emptyHand(d, me)
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe true
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 20
        d.getGraveyard(me).contains(cutthroat) shouldBe true
        d.getGraveyard(opponent).contains(bear) shouldBe true
    }
    test("shadow and flying both constrain blockers") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val cutthroat = d.putCreatureOnBattlefield(me, "Cutthroat il-Dal")
        val shadow = d.putCreatureOnBattlefield(opponent, "Soltari Foot Soldier")
        val flyer = d.putCreatureOnBattlefield(opponent, "Wind Drake")
        val embrace = d.putPermanentOnBattlefield(me, "Dauthi Embrace")
        emptyHand(d, me)
        val jump = d.putCardInHand(me, "Jump"); d.giveMana(me, Color.BLUE)
        d.castSpell(me, jump, listOf(cutthroat)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.giveMana(me, Color.BLACK, 2)
        d.submit(ActivateAbility(me, embrace, DauthiEmbrace.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Permanent(flyer)))).outcome shouldBe Outcome.Done
        d.bothPass()
        attack(d, me, cutthroat)
        val before = d.state
        d.declareBlockers(opponent, mapOf(shadow to listOf(cutthroat))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.declareBlockers(opponent, mapOf(flyer to listOf(cutthroat))).outcome shouldBe Outcome.Done
    }
    test("losing shadow after a shadow block does not undo that block") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val cutthroat = d.putCreatureOnBattlefield(me, "Cutthroat il-Dal")
        val shadow = d.putCreatureOnBattlefield(opponent, "Soltari Foot Soldier")
        emptyHand(d, me)
        attack(d, me, cutthroat)
        d.declareBlockers(opponent, mapOf(shadow to listOf(cutthroat))).outcome shouldBe Outcome.Done
        d.passPriority(opponent).outcome shouldBe Outcome.Done
        d.state.step shouldBe Step.DECLARE_BLOCKERS
        val draw = d.putCardInHand(me, "Reach Through Mists"); d.giveMana(me, Color.BLUE)
        d.castSpell(me, draw).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 1
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe false
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 20
        d.getGraveyard(me).contains(cutthroat) shouldBe true
        d.getGraveyard(opponent).contains(shadow) shouldBe true
    }
    test("an empty-handed defender's Cutthroat can block only shadow attackers") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val cutthroat = d.putCreatureOnBattlefield(opponent, "Cutthroat il-Dal")
        val normal = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val shadow = d.putCreatureOnBattlefield(me, "Soltari Foot Soldier")
        d.passPriority(me).outcome shouldBe Outcome.Done
        emptyHand(d, opponent)
        d.state.projectedState.hasKeyword(cutthroat, Keyword.SHADOW) shouldBe true
        d.getHand(me).isNotEmpty() shouldBe true
        d.removeSummoningSickness(normal); d.removeSummoningSickness(shadow)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(normal, shadow), opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val before = d.state
        d.declareBlockers(opponent, mapOf(cutthroat to listOf(normal))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.declareBlockers(opponent, mapOf(cutthroat to listOf(shadow))).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 18
        d.getGraveyard(opponent).contains(cutthroat) shouldBe true
        d.getGraveyard(me).contains(shadow) shouldBe true
    }
    test("an additional shadow grant stays redundant with hellbent shadow") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val cutthroat = d.putCreatureOnBattlefield(me, "Cutthroat il-Dal")
        val embrace = d.putPermanentOnBattlefield(me, "Dauthi Embrace")
        val shadow = d.putCreatureOnBattlefield(opponent, "Soltari Foot Soldier")
        emptyHand(d, me)
        d.giveMana(me, Color.BLACK, 2)
        d.submit(ActivateAbility(me, embrace, DauthiEmbrace.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Permanent(cutthroat)))).outcome shouldBe Outcome.Done
        d.bothPass()
        attack(d, me, cutthroat)
        d.declareBlockers(opponent, mapOf(shadow to listOf(cutthroat))).outcome shouldBe Outcome.Done
    }
})

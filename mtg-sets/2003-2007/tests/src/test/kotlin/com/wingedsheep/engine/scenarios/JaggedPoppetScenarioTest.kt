package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.JaggedPoppet
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class JaggedPoppetScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(JaggedPoppet)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun emptyHand(d: GameTestDriver, player: EntityId) {
        val discard = d.putCardInHand(player, "One with Nothing")
        d.giveMana(player, Color.BLACK)
        d.castSpell(player, discard).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(player).size shouldBe 0
    }
    fun hitPlayer(d: GameTestDriver, me: EntityId, poppet: EntityId) {
        val opponent = d.getOpponent(me)
        d.removeSummoningSickness(poppet)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(poppet), opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage()
        d.getLifeTotal(opponent) shouldBe 17
    }
    test("normal casting pays black and red and creates the printed 3-4 creature") {
        val d = setup(); val me = d.activePlayer!!
        val poppet = d.putCardInHand(me, "Jagged Poppet")
        d.giveMana(me, Color.BLACK, 2); d.giveMana(me, Color.RED)
        d.castSpell(me, poppet).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(poppet) shouldBe true
        d.state.projectedState.getPower(poppet) shouldBe 3
        d.state.projectedState.getToughness(poppet) shouldBe 4
    }
    for (spell in listOf("Shock", "Flame Slash")) {
        test("$spell damage makes its controller discard the event amount even if lethal") {
            val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
            val poppet = d.putCreatureOnBattlefield(me, "Jagged Poppet")
            val burn = d.putCardInHand(me, spell)
            d.giveMana(me, Color.RED)
            d.castSpell(me, burn, listOf(poppet)).outcome shouldBe Outcome.Done
            val hand = d.getHand(me).toList(); val otherHand = d.getHand(opponent).toList()
            d.bothPass()
            val amount = if (spell == "Shock") 2 else 4
            d.getGraveyard(me).contains(poppet) shouldBe (amount == 4)
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe me
            decision.minSelections shouldBe amount
            decision.maxSelections shouldBe amount
            val selected = hand.take(amount)
            d.submitCardSelection(me, selected).error shouldBe null
            d.getHand(me).size shouldBe hand.size - amount
            selected.all { it in d.getGraveyard(me) } shouldBe true
            d.getHand(opponent) shouldBe otherHand
        }
    }
    test("damage with an empty hand resolves the mandatory discard without a choice") {
        val d = setup(); val me = d.activePlayer!!
        val poppet = d.putCreatureOnBattlefield(me, "Jagged Poppet")
        emptyHand(d, me)
        val shock = d.putCardInHand(me, "Shock"); d.giveMana(me, Color.RED)
        d.castSpell(me, shock, listOf(poppet)).outcome shouldBe Outcome.Done
        d.bothPass(); d.bothPass()
        d.pendingDecision shouldBe null
        d.getHand(me).size shouldBe 0
        d.getPermanents(me).contains(poppet) shouldBe true
    }
    test("hellbent combat damage makes the damaged player choose exactly three discards") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val poppet = d.putCreatureOnBattlefield(me, "Jagged Poppet")
        emptyHand(d, me)
        val hand = d.getHand(opponent).toList()
        hitPlayer(d, me, poppet)
        d.state.stack.size shouldBe 1
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponent
        decision.minSelections shouldBe 3
        d.submitCardSelection(opponent, hand.take(3)).error shouldBe null
        d.getHand(opponent).size shouldBe hand.size - 3
        d.getHand(me).size shouldBe 0
    }
    test("a nonempty hand at combat damage prevents the hellbent trigger") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val poppet = d.putCreatureOnBattlefield(me, "Jagged Poppet")
        val hand = d.getHand(opponent).toList()
        hitPlayer(d, me, poppet)
        d.state.stack.size shouldBe 0
        d.pendingDecision shouldBe null
        d.getHand(opponent) shouldBe hand
    }
    test("drawing in response turns off the intervening condition at resolution") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val poppet = d.putCreatureOnBattlefield(me, "Jagged Poppet")
        emptyHand(d, me)
        val hand = d.getHand(opponent).toList()
        hitPlayer(d, me, poppet)
        d.state.stack.size shouldBe 1
        val draw = d.putCardInHand(me, "Reach Through Mists"); d.giveMana(me, Color.BLUE)
        d.castSpell(me, draw).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 1
        d.bothPass()
        d.pendingDecision shouldBe null
        d.getHand(opponent) shouldBe hand
        d.state.stack.size shouldBe 0
    }
})

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.BloodScrivener
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BloodScrivenerScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(BloodScrivener)
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun emptyHand(d: GameTestDriver, me: EntityId) {
        val discard = d.putCardInHand(me, "One with Nothing")
        d.giveMana(me, Color.BLACK)
        d.castSpell(me, discard).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 0
    }
    fun resolveChoices(d: GameTestDriver) {
        var choices = 0
        while (d.pendingDecision != null) {
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            (++choices <= 10) shouldBe true
            d.submitDecision(decision.playerId, OptionChosenResponse(decision.id, 0)).outcome.let {
                (it is Outcome.Rejected) shouldBe false
            }
        }
    }
    fun drawWithStone(d: GameTestDriver, me: EntityId) {
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, stone, MindStone.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        resolveChoices(d)
        d.getGraveyard(me).contains(stone) shouldBe true
    }
    test("normal casting pays one and black and resolves as a 2-1 creature") {
        val d = setup(); val me = d.activePlayer!!
        val scrivener = d.putCardInHand(me, "Blood Scrivener")
        d.giveMana(me, Color.BLACK, 2)
        d.castSpell(me, scrivener).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(scrivener) shouldBe true
        d.state.projectedState.getPower(scrivener) shouldBe 2
        d.state.projectedState.getToughness(scrivener) shouldBe 1
    }
    test("printed cost rejects with only one mana without changing state") {
        val d = setup(); val me = d.activePlayer!!
        val scrivener = d.putCardInHand(me, "Blood Scrivener")
        d.giveMana(me, Color.BLACK)
        val before = d.state
        d.castSpell(me, scrivener).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("an actual empty-hand draw becomes two cards and one life lost") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Blood Scrivener")
        emptyHand(d, me)
        drawWithStone(d, me)
        d.getHand(me).size shouldBe 2
        d.getLifeTotal(me) shouldBe 19
        d.getLifeTotal(opponent) shouldBe 20
    }
    test("a nonempty hand leaves the draw and life total unchanged") {
        val d = setup(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Blood Scrivener")
        emptyHand(d, me)
        d.putCardInHand(me, "Mountain")
        drawWithStone(d, me)
        d.getHand(me).size shouldBe 2
        d.getLifeTotal(me) shouldBe 20
    }
    test("a real two-card spell draws three total because draws happen one at a time") {
        val d = setup(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Blood Scrivener")
        emptyHand(d, me)
        val draw = d.putCardInHand(me, "Divination")
        d.giveMana(me, Color.BLUE, 3)
        d.castSpell(me, draw).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 3
        d.getLifeTotal(me) shouldBe 19
    }
    for (count in listOf(2, 3)) test("$count replacement sources add one card and one lost life each without recursion") {
        val d = setup(); val me = d.activePlayer!!
        repeat(count) { d.putCreatureOnBattlefield(me, "Blood Scrivener") }
        emptyHand(d, me)
        drawWithStone(d, me)
        d.getHand(me).size shouldBe count + 1
        d.getLifeTotal(me) shouldBe 20 - count
        d.pendingDecision shouldBe null
    }
    test("an opponent's Scrivener does not replace the controller's draw") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(opponent, "Blood Scrivener")
        emptyHand(d, me)
        drawWithStone(d, me)
        d.getHand(me).size shouldBe 1
        d.getLifeTotal(me) shouldBe 20
        d.getLifeTotal(opponent) shouldBe 20
    }
    test("a real draw-step draw uses the same empty-hand replacement") {
        val d = setup(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Blood Scrivener")
        emptyHand(d, me)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.DRAW)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.DRAW)
        d.state.activePlayerId shouldBe me
        d.getHand(me).size shouldBe 2
        d.getLifeTotal(me) shouldBe 19
    }
    test("destroying the source in response restores the ordinary draw") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val scrivener = d.putCreatureOnBattlefield(me, "Blood Scrivener")
        emptyHand(d, me)
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, stone, MindStone.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.passPriority(me)
        val shock = d.putCardInHand(opponent, "Shock")
        d.giveMana(opponent, Color.RED)
        d.castSpell(opponent, shock, listOf(scrivener)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(me).contains(scrivener) shouldBe true
        d.bothPass()
        d.getHand(me).size shouldBe 1
        d.getLifeTotal(me) shouldBe 20
    }
})

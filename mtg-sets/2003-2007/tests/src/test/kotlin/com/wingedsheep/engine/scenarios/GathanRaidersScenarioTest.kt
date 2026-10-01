package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fut.cards.GathanRaiders
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GathanRaidersScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(GathanRaiders)
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
    fun faceDown(d: GameTestDriver, me: EntityId): EntityId {
        val raiders = d.putCardInHand(me, "Gathan Raiders")
        d.giveColorlessMana(me, 3)
        d.submit(CastSpell(me, raiders, castFaceDown = true, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(raiders)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        d.state.projectedState.getPower(raiders) shouldBe 2
        d.state.projectedState.getToughness(raiders) shouldBe 2
        return raiders
    }
    test("normal casting pays printed mana and hellbent follows hand changes") {
        val d = setup(); val me = d.activePlayer!!
        val raiders = d.putCardInHand(me, "Gathan Raiders")
        d.giveMana(me, Color.RED, 5)
        d.castSpell(me, raiders).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getPower(raiders) shouldBe 3
        emptyHand(d, me)
        d.state.projectedState.getPower(raiders) shouldBe 5
        d.state.projectedState.getToughness(raiders) shouldBe 5
        d.putCardInHand(me, "Mountain")
        d.state.projectedState.getPower(raiders) shouldBe 3
    }
    test("face-down casting hides hellbent even when the hand becomes empty") {
        val d = setup(); val me = d.activePlayer!!
        emptyHand(d, me)
        val raiders = faceDown(d, me)
        d.getHand(me).size shouldBe 0
        d.state.projectedState.getPower(raiders) shouldBe 2
        val before = d.state
        d.submit(TurnFaceUp(me, raiders)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("printed casting cost cannot be paid with only four mana") {
        val d = setup(); val me = d.activePlayer!!
        val raiders = d.putCardInHand(me, "Gathan Raiders")
        d.giveMana(me, Color.RED, 4)
        val before = d.state
        d.castSpell(me, raiders).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("drawing a card makes three marked damage lethal when hellbent ends") {
        val d = setup(); val me = d.activePlayer!!
        val raiders = d.putCreatureOnBattlefield(me, "Gathan Raiders")
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        emptyHand(d, me)
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED)
        d.castSpell(me, bolt, listOf(raiders)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(raiders) shouldBe true
        d.state.projectedState.getToughness(raiders) shouldBe 5
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, stone, MindStone.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 1
        d.getGraveyard(me).contains(raiders) shouldBe true
        d.getGraveyard(me).contains(stone) shouldBe true
    }
    test("hellbent changes to the new controller's hand after Control Magic resolves") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val raiders = d.putCreatureOnBattlefield(opponent, "Gathan Raiders")
        emptyHand(d, me)
        d.getHand(opponent).isNotEmpty() shouldBe true
        d.state.projectedState.getPower(raiders) shouldBe 3
        val control = d.putCardInHand(me, "Control Magic")
        d.giveMana(me, Color.BLUE, 4)
        d.castSpell(me, control, listOf(raiders)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.getController(raiders) shouldBe me
        d.getHand(me).size shouldBe 0
        d.state.projectedState.getPower(raiders) shouldBe 5
        d.putCardInHand(me, "Mountain")
        d.state.projectedState.getPower(raiders) shouldBe 3
    }
    for (remaining in listOf(0, 1)) {
        test("morph discards exactly one card with $remaining cards remaining") {
            val d = setup(); val me = d.activePlayer!!
            emptyHand(d, me)
            val raiders = faceDown(d, me)
            val payment = d.putCardInHand(me, "Mountain")
            repeat(remaining) { d.putCardInHand(me, "Mountain") }
            d.submit(TurnFaceUp(me, raiders)).error shouldBe null
            d.state.getEntity(raiders)?.get<FaceDownComponent>() shouldBe FaceDownComponent
            d.submitCardSelection(me, listOf(payment)).error shouldBe null
            d.getGraveyard(me).contains(payment) shouldBe true
            d.getHand(me).size shouldBe remaining
            d.state.getEntity(raiders)?.get<FaceDownComponent>() shouldBe null
            d.state.projectedState.getPower(raiders) shouldBe if (remaining == 0) 5 else 3
            d.state.stack.size shouldBe 0
        }
    }
})

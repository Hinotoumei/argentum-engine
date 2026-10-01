package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.stx.cards.TheBiblioplex
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TheBiblioplexScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(TheBiblioplex)
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun hand(d: GameTestDriver, me: EntityId, count: Int) {
        val discard = d.putCardInHand(me, "One with Nothing")
        d.giveMana(me, Color.BLACK)
        d.castSpell(me, discard).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 0
        repeat(count) { d.putCardInHand(me, "Mountain") }
    }
    fun activate(d: GameTestDriver, me: EntityId, land: EntityId) {
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        d.isTapped(land) shouldBe true
        d.bothPass()
    }
    fun choose(d: GameTestDriver, me: EntityId, selected: List<EntityId>) {
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe me
        d.submitCardSelection(me, selected).outcome.let { (it is Outcome.Rejected) shouldBe false }
    }
    for (count in listOf(0, 7)) test("lookup activates with exactly $count cards and may leave a nonmatching card on top") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, count)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, "Mountain")
        activate(d, me, land)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(top)
        choose(d, me, emptyList())
        d.state.getLibrary(me).first() shouldBe top
        d.getHand(me).size shouldBe count
    }
    for (count in listOf(1, 6, 8)) test("lookup at $count cards rejects without paying or tapping") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, count)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        d.giveColorlessMana(me, 2)
        val before = d.state
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    for (name in listOf("Shock", "Divination")) test("matching $name is privately looked at then revealed into hand") {
        val d = setup(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, name)
        activate(d, me, land)
        val view = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        view.transform(d.state, viewingPlayerId = me).cards.containsKey(top) shouldBe true
        view.transform(d.state, viewingPlayerId = opponent).cards.containsKey(top) shouldBe false
        choose(d, me, listOf(top))
        d.getHand(me) shouldBe listOf(top)
        d.pendingDecision shouldBe null
        view.transform(d.state, viewingPlayerId = opponent).cards[top]?.name shouldBe name
    }
    for (mill in listOf(false, true)) test("declining a matching card independently permits graveyard choice $mill") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, "Shock")
        activate(d, me, land)
        choose(d, me, emptyList())
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(top)
        choose(d, me, if (mill) listOf(top) else emptyList())
        d.getGraveyard(me).contains(top) shouldBe mill
        d.state.getLibrary(me).contains(top) shouldBe !mill
        d.getHand(me).size shouldBe 0
    }
    test("a nonmatching creature may go to the graveyard") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        activate(d, me, land)
        choose(d, me, listOf(top))
        d.getGraveyard(me).contains(top) shouldBe true
        d.getHand(me).size shouldBe 0
    }
    test("a real response draw changes hand size without disabling resolution") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, "Shock")
        d.putCardOnTopOfLibrary(me, "Island")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        val draw = d.putCardInHand(me, "Reach Through Mists")
        d.giveMana(me, Color.BLUE)
        d.castSpell(me, draw).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHand(me).size shouldBe 1
        d.bothPass()
        choose(d, me, listOf(top))
        d.getHand(me).size shouldBe 2
        d.getHand(me).contains(top) shouldBe true
    }
    test("exiling the source in response does not cancel its lookup") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val top = d.putCardOnTopOfLibrary(me, "Shock")
        d.giveColorlessMana(me, 2)
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        val removal = d.putCardInHand(me, "Scour from Existence")
        d.giveColorlessMana(me, 7)
        d.castSpell(me, removal, listOf(land)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(land) shouldBe false
        d.bothPass()
        choose(d, me, listOf(top))
        d.getHand(me).contains(top) shouldBe true
    }
    test("empty library makes lookup finish without a choice or draw loss") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        var state = d.state
        state.getLibrary(me).forEach { state = state.removeFromZone(ZoneKey(me, Zone.LIBRARY), it) }
        d.replaceState(state)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        activate(d, me, land)
        d.pendingDecision shouldBe null
        d.getLifeTotal(me) shouldBe 20
        d.getHand(me).size shouldBe 0
    }
    test("unaffordable lookup rejects atomically") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putPermanentOnBattlefield(me, "The Biblioplex")
        val before = d.state
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("normal land play supplies colorless mana immediately and tapped lookup rejects") {
        val d = setup(); val me = d.activePlayer!!
        hand(d, me, 0)
        val land = d.putCardInHand(me, "The Biblioplex")
        d.submit(PlayLand(me, land)).outcome shouldBe Outcome.Done
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.isTapped(land) shouldBe true
        d.stackSize shouldBe 0
        val ring = d.putCardInHand(me, "Sol Ring")
        d.castSpell(me, ring).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getPermanents(me).contains(ring) shouldBe true
        hand(d, me, 0)
        d.giveColorlessMana(me, 2)
        val before = d.state
        d.submit(ActivateAbility(me, land, TheBiblioplex.activatedAbilities[1].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

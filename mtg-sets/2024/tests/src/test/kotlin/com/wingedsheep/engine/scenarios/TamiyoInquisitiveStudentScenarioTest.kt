package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TamiyoInquisitiveStudent
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TamiyoInquisitiveStudentScenarioTest : FunSpec({
    val cards = TestCards.all + listOf(TamiyoInquisitiveStudent, PredefinedTokens.Clue)
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun settle(d: GameTestDriver) {
        var passes = 0
        while (d.stackSize > 0 && d.state.pendingDecision == null && passes++ < 24) {
            d.passPriority(d.priorityPlayer!!).error shouldBe null
        }
        (passes < 24) shouldBe true
    }
    fun student(d: GameTestDriver): EntityId {
        val me = d.activePlayer!!
        val id = d.putCardInHand(me, "Tamiyo, Inquisitive Student")
        d.giveMana(me, Color.BLUE, 1)
        d.castSpell(me, id).error shouldBe null
        settle(d)
        return id
    }
    fun drawOne(d: GameTestDriver, player: EntityId) {
        if (d.priorityPlayer != player) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val spell = d.putCardInHand(player, "Think Twice")
        d.giveMana(player, Color.BLUE, 2)
        d.castSpell(player, spell).error shouldBe null
        settle(d)
    }
    fun scholar(d: GameTestDriver): EntityId {
        val id = student(d); repeat(3) { drawOne(d, d.activePlayer!!) }
        d.state.getEntity(id)?.get<CardComponent>()?.name shouldBe "Tamiyo, Seasoned Scholar"
        return id
    }
    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
    fun giveLoyalty(d: GameTestDriver, id: EntityId, amount: Int) {
        d.addComponent(id, CountersComponent().withAdded(CounterType.LOYALTY, amount))
    }
    fun activate(d: GameTestDriver, id: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = TamiyoInquisitiveStudent.backFace!!.script.activatedAbilities[index]
        d.submit(ActivateAbility(d.activePlayer!!, id, ability.id, targets = targets)).error shouldBe null
        settle(d)
    }

    test("real blue casting enters a flying 0/3 front face; unaffordable casting is atomic") {
        val d = setup(); val me = d.activePlayer!!
        val card = d.putCardInHand(me, "Tamiyo, Inquisitive Student"); val before = d.state
        d.castSpell(me, card).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.giveMana(me, Color.BLUE, 1); d.castSpell(me, card).error shouldBe null; settle(d)
        d.state.projectedState.getPower(card) shouldBe 0
        d.state.projectedState.getToughness(card) shouldBe 3
        d.state.projectedState.hasKeyword(card, Keyword.FLYING) shouldBe true
    }
    for (saved in listOf(false, true)) test("the third real draw exiles and returns with exactly two loyalty; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val id = student(d)
        repeat(2) { drawOne(d, me) }
        d.state.getEntity(id)?.get<CardComponent>()?.name shouldBe "Tamiyo, Inquisitive Student"
        val spell = d.putCardInHand(me, "Think Twice"); d.giveMana(me, Color.BLUE, 2)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass()
        d.stackSize shouldBe 1
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        settle(d)
        d.state.getEntity(id)?.get<CardComponent>()?.name shouldBe "Tamiyo, Seasoned Scholar"
        loyalty(d, id) shouldBe 2
        val moves = d.events.filterIsInstance<ZoneChangeEvent>().filter { it.entityId == id }
        moves.count { it.toZone == Zone.EXILE } shouldBe 1
        moves.count { it.fromZone == Zone.EXILE && it.toZone == Zone.BATTLEFIELD } shouldBe 1
        d.state.getEntity(id)?.get<ControllerComponent>()?.playerId shouldBe me
    }
    test("a batch crossing the third draw triggers once, and a stolen Tamiyo returns to her owner") {
        val d = setup(); val owner = d.activePlayer!!; val other = d.getOpponent(owner)
        val id = student(d)
        d.addComponent(id, ControllerComponent(other))
        drawOne(d, other)
        val spell = d.putCardInHand(other, "Divination")
        // Divination is a sorcery: advance to that player's main phase for a legal real cast.
        d.passPriorityUntil(Step.END); d.bothPass(); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe other
        d.giveMana(other, Color.BLUE, 3)
        d.castSpell(other, spell).error shouldBe null; settle(d)
        // This turn's normal draw was card one; Divination crosses to card three in one batch.
        d.state.getEntity(id)?.get<CardComponent>()?.name shouldBe "Tamiyo, Seasoned Scholar"
        loyalty(d, id) shouldBe 2
        d.state.getEntity(id)?.get<ControllerComponent>()?.playerId shouldBe owner
        d.events.filterIsInstance<ZoneChangeEvent>().count { it.entityId == id && it.toZone == Zone.EXILE } shouldBe 1
    }
    test("an actual attack investigates, and the Clue can be sacrificed to draw") {
        val d = setup(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val id = student(d)
        // Mature the genuinely cast creature through both players' turns.
        d.passPriorityUntil(Step.END); d.bothPass(); d.passPriorityUntil(Step.END); d.bothPass()
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.activePlayer shouldBe me
        d.declareAttackers(me, listOf(id), other).error shouldBe null; settle(d)
        val clues = d.state.getBattlefield().filter { d.state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }
        clues.size shouldBe 1
        val clue = clues.single(); val before = d.getHand(me).size
        d.giveMana(me, Color.BLUE, 2)
        val ability = PredefinedTokens.Clue.script.activatedAbilities.single()
        d.submit(ActivateAbility(me, clue, ability.id)).error shouldBe null; settle(d)
        d.state.getBattlefield().contains(clue) shouldBe false
        d.getHand(me).size shouldBe before + 1
    }
    test("+2 works immediately after the exile-return transform, once per turn") {
        val d = setup(); val id = scholar(d)
        activate(d, id, 0)
        loyalty(d, id) shouldBe 4
        val before = d.state
        d.submit(ActivateAbility(d.activePlayer!!, id, TamiyoInquisitiveStudent.backFace!!.script.activatedAbilities[0].id))
            .outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    for (name in listOf("Think Twice", "Regrowth")) test("-3 returns $name and adds mana only for a green returned card") {
        val d = setup(); val me = d.activePlayer!!; val id = scholar(d); giveLoyalty(d, id, 3)
        val target = d.putCardInGraveyard(me, name)
        activate(d, id, 1, listOf(ChosenTarget.Card(target, ownerId = me, zone = Zone.GRAVEYARD)))
        d.getHand(me).contains(target) shouldBe true
        d.getGraveyard(me).contains(target) shouldBe false
        if (name == "Regrowth") {
            val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
            d.submitDecision(me, ColorChosenResponse(decision.id, Color.RED)).error shouldBe null
            d.state.getEntity(me)?.get<ManaPoolComponent>()?.red shouldBe 1
        } else d.state.pendingDecision shouldBe null
        // Spending the last loyalty puts the planeswalker in the graveyard, without stopping its ability.
        d.getGraveyard(me).contains(id) shouldBe true
    }
    for (librarySize in listOf(0, 5, 6)) test("-7 draws rounded-up half of library size $librarySize and creates a persistent emblem") {
        val d = setup(); val me = d.activePlayer!!; val id = scholar(d); giveLoyalty(d, id, 7)
        var state = d.state
        for (card in state.getLibrary(me).drop(librarySize)) {
            state = state.removeFromZone(com.wingedsheep.engine.state.ZoneKey(me, Zone.LIBRARY), card)
        }
        d.replaceState(state)
        val hand = d.getHand(me).size
        activate(d, id, 2)
        d.getHand(me).size shouldBe hand + (librarySize + 1) / 2
        d.state.getLibrary(me).size shouldBe librarySize / 2
        d.getGraveyard(me).contains(id) shouldBe true
        d.state.gameOver shouldBe false
        // Fill the hand past seven, then verify cleanup does not ask for discard after Tamiyo has left.
        repeat(10) { d.putCardInHand(me, "Island") }
        d.passPriorityUntil(Step.END); d.bothPass(); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe d.getOpponent(me)
        d.getHand(me).size shouldBe hand + (librarySize + 1) / 2 + 10
    }

    for (saved in listOf(false, true)) test("+2 weakens each attacker of you or your planeswalker, including after Tamiyo leaves; saved=$saved") {
        val d = setup(); val me = d.activePlayer!!; val other = d.getOpponent(me); val id = scholar(d)
        activate(d, id, 0)
        val walker = d.putPermanentOnBattlefield(me, "Jace Beleren")
        d.addComponent(walker, CountersComponent().withAdded(CounterType.LOYALTY, 3))
        val first = d.putPermanentOnBattlefield(other, "Grizzly Bears")
        val second = d.putPermanentOnBattlefield(other, "Grizzly Bears")
        d.removeSummoningSickness(first); d.removeSummoningSickness(second)
        d.passPriorityUntil(Step.END); d.bothPass(); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe other
        val removal = d.putCardInHand(other, "Vindicate")
        d.giveMana(other, Color.WHITE, 2); d.giveMana(other, Color.BLACK, 1)
        d.castSpell(other, removal, listOf(id)).error shouldBe null; settle(d)
        d.getGraveyard(me).contains(id) shouldBe true
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(other, mapOf(first to me, second to walker)).error shouldBe null
        d.stackSize shouldBe 2
        settle(d)
        d.state.projectedState.getPower(first) shouldBe 1
        d.state.projectedState.getPower(second) shouldBe 1
        d.passPriorityUntil(Step.END)
        d.bothPass(); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.projectedState.getPower(first) shouldBe 2
        d.state.projectedState.getPower(second) shouldBe 2
        // At the controller's next turn, the watcher has expired. A later opposing attack is normal.
        d.passPriorityUntil(Step.END); d.bothPass(); d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(other, listOf(first, second), me).error shouldBe null
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(first) shouldBe 2
        d.state.projectedState.getPower(second) shouldBe 2
    }
    test("the third-draw trigger does not bring Tamiyo back after a response exiles her") {
        val d = setup(); val me = d.activePlayer!!; val other = d.getOpponent(me); val id = student(d)
        repeat(2) { drawOne(d, me) }
        val draw = d.putCardInHand(me, "Think Twice"); d.giveMana(me, Color.BLUE, 2)
        d.castSpell(me, draw).error shouldBe null; d.bothPass(); d.stackSize shouldBe 1
        d.passPriority(me).error shouldBe null
        val exile = d.putCardInHand(other, "Swords to Plowshares"); d.giveMana(other, Color.WHITE, 1)
        d.castSpell(other, exile, listOf(id)).error shouldBe null; settle(d)
        d.state.getExile(me).contains(id) shouldBe true
        d.state.getBattlefield().contains(id) shouldBe false
    }
})

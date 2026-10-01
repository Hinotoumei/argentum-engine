package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ddj.cards.IzzetCharm
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class IzzetCharmScenarioTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(IzzetCharm))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun prepare(d: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.sdk.model.EntityId {
        d.giveMana(player, Color.BLUE)
        d.giveMana(player, Color.RED)
        return d.putCardInHand(player, "Izzet Charm")
    }

    for (pay in listOf(false, true)) {
        test("counter mode resolves with opponent payment $pay") {
            val d = setup()
            val caster = d.activePlayer!!
            val me = d.getOpponent(caster)
            val spell = d.putCardInHand(caster, "Mind Stone")
            d.giveMana(caster, Color.GREEN, 2)
            d.castSpell(caster, spell).outcome shouldBe Outcome.Done
            d.passPriority(caster)
            val charm = prepare(d, me)
            val target = ChosenTarget.Spell(d.getTopOfStack()!!)
            d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(target)))).outcome shouldBe Outcome.Done
            if (pay) d.giveMana(caster, Color.GREEN, 2)
            d.bothPass()
            if (d.pendingDecision != null) d.submitYesNo(caster, pay)
            if (pay) d.bothPass()
            d.state.getBattlefield().contains(spell) shouldBe pay
            d.getGraveyard(caster).contains(spell) shouldBe !pay
        }
    }

    test("damage mode destroys a two-toughness creature") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val charm = prepare(d, me)
        val target = ChosenTarget.Permanent(creature)
        d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(target)))).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(opponent).contains(creature) shouldBe true
    }

    test("draw mode has no targets and allows discarding the newly drawn cards") {
        val d = setup()
        val me = d.activePlayer!!
        val charm = prepare(d, me)
        val first = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val second = d.putCardOnTopOfLibrary(me, "Hill Giant")
        val handSize = d.getHand(me).size
        d.submit(CastSpell(me, charm, chosenModes = listOf(2))).outcome shouldBe Outcome.Done
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.containsAll(listOf(first, second)) shouldBe true
        d.getHand(me).size shouldBe handSize + 1
        val shock = d.putCardInHand(me, "Shock")
        d.giveMana(me, Color.RED)
        val beforeInterrupt = d.state
        d.castSpell(me, shock, listOf(d.getOpponent(me))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe beforeInterrupt
        d.submitCardSelection(me, listOf(first, second))
        d.getGraveyard(me).containsAll(listOf(first, second, charm)) shouldBe true
        d.getHand(me).size shouldBe handSize
    }

    for (modes in listOf(listOf(0, 2), listOf(3), listOf(2, 2))) {
        test("invalid modes $modes reject without spending mana") {
            val d = setup()
            val me = d.activePlayer!!
            val charm = prepare(d, me)
            val before = d.state
            d.submit(CastSpell(me, charm, chosenModes = modes)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe before
        }
    }

    test("damage mode cannot target a player") {
        val d = setup()
        val me = d.activePlayer!!
        val charm = prepare(d, me)
        val target = ChosenTarget.Player(d.getOpponent(me))
        val before = d.state
        d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }

    test("counter mode cannot target a creature spell") {
        val d = setup()
        val me = d.activePlayer!!
        val creature = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, creature).outcome shouldBe Outcome.Done
        val charm = prepare(d, me)
        val target = ChosenTarget.Spell(d.getTopOfStack()!!)
        val before = d.state
        d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }

    test("counter mode cannot target an activated ability on the stack") {
        val d = setup()
        val me = d.activePlayer!!
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        d.giveMana(me, Color.GREEN)
        val ability = d.cardRegistry.getCard("Mind Stone")!!.script.activatedAbilities.last()
        d.submit(ActivateAbility(me, stone, ability.id)).outcome shouldBe Outcome.Done
        val charm = prepare(d, me)
        val target = ChosenTarget.Spell(d.getTopOfStack()!!)
        val before = d.state
        d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }

    test("damage mode cannot target a spell on the stack") {
        val d = setup()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        val charm = prepare(d, me)
        val target = ChosenTarget.Spell(d.getTopOfStack()!!)
        val before = d.state
        d.submit(CastSpell(me, charm, targets = listOf(target), chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

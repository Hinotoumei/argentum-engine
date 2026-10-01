package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.Hydroblast
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HydroblastScenarioTest : FunSpec({
    val colorShifts = listOf(Color.RED, Color.GREEN).map { color ->
        card("Test Hydroblast Shift ${color.name}") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.ChangeColor(permanent, setOf(color))
            }
        }
    }
    val cards = TestCards.all + listOf(Hydroblast) + colorShifts
    fun setup() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, me: EntityId, mode: Int, target: ChosenTarget): ExecutionResult {
        val hydro = d.putCardInHand(me, "Hydroblast")
        d.giveMana(me, Color.BLUE)
        return d.submit(CastSpell(me, hydro, targets = listOf(target), chosenModes = listOf(mode),
            modeTargetsOrdered = listOf(listOf(target))))
    }

    for (red in listOf(false, true)) {
        test("counter mode legally targets a spell whose red status is $red") {
            val d = setup()
            val caster = d.activePlayer!!
            val me = d.getOpponent(caster)
            val spell = d.putCardInHand(caster, if (red) "Shock" else "Mind Stone")
            d.giveMana(caster, if (red) Color.RED else Color.GREEN, 2)
            d.castSpell(caster, spell, if (red) listOf(me) else emptyList()).outcome shouldBe Outcome.Done
            d.passPriority(caster)
            cast(d, me, 0, ChosenTarget.Spell(d.getTopOfStack()!!)).outcome shouldBe Outcome.Done
            d.bothPass()
            if (!red) d.bothPass()
            d.getLifeTotal(me) shouldBe 20
            d.getGraveyard(caster).contains(spell) shouldBe red
            d.state.getBattlefield().contains(spell) shouldBe !red
        }
        test("destroy mode legally targets a permanent whose red status is $red") {
            val d = setup()
            val me = d.activePlayer!!
            val opponent = d.getOpponent(me)
            val permanent = d.putCreatureOnBattlefield(opponent, if (red) "Hill Giant" else "Grizzly Bears")
            cast(d, me, 1, ChosenTarget.Permanent(permanent)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.getGraveyard(opponent).contains(permanent) shouldBe red
            d.state.getBattlefield().contains(permanent) shouldBe !red
        }
        test("destroy mode uses the target's color after a real color-change spell resolves: $red") {
            val d = setup()
            val me = d.activePlayer!!
            val opponent = d.getOpponent(me)
            val permanent = d.putCreatureOnBattlefield(opponent, if (red) "Grizzly Bears" else "Hill Giant")
            cast(d, me, 1, ChosenTarget.Permanent(permanent)).outcome shouldBe Outcome.Done
            val color = if (red) Color.RED else Color.GREEN
            val shift = d.putCardInHand(me, "Test Hydroblast Shift ${color.name}")
            d.giveMana(me, Color.BLUE)
            d.castSpell(me, shift, listOf(permanent)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.bothPass()
            d.getGraveyard(opponent).contains(permanent) shouldBe red
            d.state.getBattlefield().contains(permanent) shouldBe !red
        }
    }
    test("counter mode rejects an activated ability without paying its costs") {
        val d = setup()
        val me = d.activePlayer!!
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        d.giveMana(me, Color.GREEN)
        val ability = d.cardRegistry.getCard("Mind Stone")!!.script.activatedAbilities.last()
        d.submit(ActivateAbility(me, stone, ability.id)).outcome shouldBe Outcome.Done
        val hydro = d.putCardInHand(me, "Hydroblast")
        d.giveMana(me, Color.BLUE)
        val target = ChosenTarget.Spell(d.getTopOfStack()!!)
        val before = d.state
        d.submit(CastSpell(me, hydro, targets = listOf(target), chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
    test("destroy mode rejects a player atomically") {
        val d = setup()
        val me = d.activePlayer!!
        val hydro = d.putCardInHand(me, "Hydroblast")
        d.giveMana(me, Color.BLUE)
        val target = ChosenTarget.Player(d.getOpponent(me))
        val before = d.state
        d.submit(CastSpell(me, hydro, targets = listOf(target), chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(target)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
    }
})

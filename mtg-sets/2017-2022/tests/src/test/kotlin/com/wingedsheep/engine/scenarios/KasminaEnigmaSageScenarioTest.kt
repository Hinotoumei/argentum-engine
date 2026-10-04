package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.KasminaEnigmaSage
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.matchers.shouldBe

class KasminaEnigmaSageScenarioTest : ScenarioTestBase() {
    init {
        test("the granted ultimate uses the activating planeswalker's colors and casts without mana") {
            val walker = card("Kasmina Blue Test Walker") {
                manaCost = "{U}"
                typeLine = "Planeswalker — Test"
                startingLoyalty = 8
            }
            val blueSpell = card("Kasmina Blue Test Spell") {
                manaCost = "{9}{U}"
                typeLine = "Instant"
                spell { effect = Effects.GainLife(2) }
            }
            val greenSpell = card("Kasmina Green Test Spell") {
                manaCost = "{G}"
                typeLine = "Instant"
                spell { effect = Effects.GainLife(1) }
            }
            val d = GameTestDriver()
            d.registerCards(TestCards.all + listOf(KasminaEnigmaSage, walker, blueSpell, greenSpell))
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            d.putPermanentOnBattlefield(me, KasminaEnigmaSage.name)
            val source = d.putPermanentOnBattlefield(me, walker.name)
            d.replaceState(d.state.updateEntity(source) {
                it.with(CountersComponent(mapOf(CounterType.LOYALTY to 8)))
            })
            val blue = d.putCardOnTopOfLibrary(me, blueSpell.name)
            val green = d.putCardOnTopOfLibrary(me, greenSpell.name)
            val abilities = d.legalActions(me).map { it.action }.filterIsInstance<ActivateAbility>()
                .filter { it.sourceId == source }
            abilities.size shouldBe 3
            // Kasmina grants +2, minus X, and minus 8 in printed order.
            d.submit(abilities.last()).error shouldBe null
            d.bothPass()
            val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            choice.options shouldContain blue
            choice.options shouldNotContain green
            val before = d.state
            d.submitCardSelection(me, listOf(green)).error shouldNotBe null
            d.state shouldBe before
            d.submitCardSelection(me, listOf(blue)).error shouldBe null
            d.submitYesNo(me, true).error shouldBe null
            d.state.stack.contains(blue) shouldBe true
            d.bothPass()
            d.getLifeTotal(me) shouldBe 22
            d.getGraveyard(me).contains(blue) shouldBe true
        }
        test("Kasmina has her printed subtype and her +2 scries one") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Kasmina, Enigma Sage")
                .withCardInLibrary(1, "Forest").withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("Kasmina, Enigma Sage")!!
            game.state.getEntity(source)!!.get<CardComponent>()!!.typeLine.hasSubtype(Subtype("Kasmina")) shouldBe true
            val ability = game.getLegalActions(1).first {
                (it.action as? ActivateAbility)?.sourceId == source && it.description.contains("Scry", ignoreCase = true)
            }
            game.execute(ability.action).error shouldBe null
            game.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
            game.resolveStack()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()
        }
        test("Kasmina grants her +2 to the empowered Jace created by Way of the Pyromancer") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Kasmina, Enigma Sage")
                .withCardInHand(1, "Way of the Pyromancer").withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest").withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Way of the Pyromancer").error shouldBe null
            game.resolveStack()
            val source = game.findPermanents("Jace").single()
            val ability = game.getLegalActions(1).first {
                (it.action as? ActivateAbility)?.sourceId == source && it.description.contains("Scry", ignoreCase = true)
            }
            game.execute(ability.action).error shouldBe null
            game.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
            game.resolveStack()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()
        }
        test("Kasmina's minus X pays loyalty and creates a Fractal with X counters") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Kasmina, Enigma Sage")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("Kasmina, Enigma Sage")!!
            val action = game.getLegalActions(1).first {
                (it.action as? ActivateAbility)?.sourceId == source && it.description.contains("Fractal")
            }.action as ActivateAbility
            game.execute(action.copy(xValue = 1)).error shouldBe null
            game.resolveStack()
            val token = game.state.getBattlefield().single {
                game.state.getEntity(it)?.get<CardComponent>()?.typeLine?.hasSubtype(Subtype("Fractal")) == true
            }
            game.state.getEntity(token)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.getEntity(source)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 1
        }
    }
}

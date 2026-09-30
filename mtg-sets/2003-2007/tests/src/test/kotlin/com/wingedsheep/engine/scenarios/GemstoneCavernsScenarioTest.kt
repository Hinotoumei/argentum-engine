package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.mtg.sets.definitions.tsp.cards.GemstoneCaverns
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class GemstoneCavernsScenarioTest : FunSpec({
    val forest = CardDefinition.basicLand("Forest", Subtype("Forest"))
    for (copies in listOf(1, 2)) {
        test("$copies opening-hand Caverns cannot leave a stale pregame prompt") {
            val registry = GameTestDriver().also {
                it.registerCards(listOf(forest, GemstoneCaverns))
            }.cardRegistry
            val init = GameInitializer(registry).initializeGame(GameConfig(
                players = listOf(
                    PlayerConfig("Caverns owner", Deck.of("Gemstone Caverns" to copies)),
                    PlayerConfig("Starting player", Deck.of("Forest" to 60))
                ), startingPlayerIndex = 1, startingHandSize = copies
            ))
            val owner = init.playerIds[0]
            val processor = ActionProcessor(registry)
            var state = processor.process(init.state, KeepHand(owner)).result.state
            state = processor.process(state, KeepHand(init.playerIds[1])).result.state
            if (copies == 1) {
                // No other card exists to satisfy the mandatory exile rider.
                state.pendingDecision shouldBe null
                state.getHand(owner).size shouldBe 1
                state.getBattlefield().size shouldBe 0
            } else {
                val offer = state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                state = processor.process(state, SubmitDecision(owner, YesNoResponse(offer.id, true))).result.state
                val exile = state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                exile.options.size shouldBe 1
                state = processor.process(state, SubmitDecision(owner, CardsSelectedResponse(exile.id, exile.options))).result.state
                // Exiling the second pending Caverns must prevent a later offer for that card.
                state.pendingDecision shouldBe null
                state.getHand(owner).size shouldBe 0
                state.getExile(owner).size shouldBe 1
                state.getBattlefield().size shouldBe 1
            }
        }
    }
    for (starting in listOf(true, false)) {
        for (accept in listOf(false, true)) {
            test("opening-hand Caverns with starting player $starting and accept $accept") {
                val registry = GameTestDriver().also {
                    it.registerCards(listOf(forest, GemstoneCaverns))
                }.cardRegistry
                val init = GameInitializer(registry).initializeGame(GameConfig(
                    players = listOf(
                        PlayerConfig("Caverns owner", Deck.of("Gemstone Caverns" to 1, "Forest" to 59)),
                        PlayerConfig("Other player", Deck.of("Forest" to 60))
                    ), startingPlayerIndex = if (starting) 0 else 1, startingHandSize = 60
                ))
                val owner = init.playerIds[0]
                val other = init.playerIds[1]
                val processor = ActionProcessor(registry)
                var state = init.state
                val cavern = state.getHand(owner).single { state.getEntity(it)?.get<CardComponent>()?.name == "Gemstone Caverns" }
                state = processor.process(state, KeepHand(owner)).result.state
                state = processor.process(state, KeepHand(other)).result.state
                if (starting) {
                    state.pendingDecision shouldBe null
                    state.getHand(owner) shouldContain cavern
                    state.getBattlefield() shouldNotContain cavern
                } else {
                    val prompt = state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                    prompt.playerId shouldBe owner
                    val result = processor.process(state, SubmitDecision(owner, YesNoResponse(prompt.id, accept))).result
                    state = result.state
                    if (accept) {
                        state.getBattlefield() shouldContain cavern
                        state.getEntity(cavern)?.get<CountersComponent>()?.getCount(CounterType.LUCK) shouldBe 1
                        result.events.any { it is CountersAddedEvent && it.entityId == cavern && it.counterType == CounterType.LUCK } shouldBe true
                        val exile = state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                        exile.options shouldNotContain cavern
                        val chosen = exile.options.first()
                        val invalid = processor.process(state, SubmitDecision(owner, CardsSelectedResponse(exile.id, listOf(cavern)))).result
                        invalid.outcome.shouldBeInstanceOf<Outcome.Rejected>()
                        invalid.state shouldBe state
                        state = processor.process(state, SubmitDecision(owner, CardsSelectedResponse(exile.id, listOf(chosen)))).result.state
                        state.getHand(owner) shouldNotContain chosen
                        state.getExile(owner) shouldContain chosen
                        state.pendingDecision shouldBe null
                    } else {
                        state.getHand(owner) shouldContain cavern
                        state.getBattlefield() shouldNotContain cavern
                        state.pendingDecision shouldBe null
                    }
                }
            }
        }
    }
    test("a normally played Caverns produces only colorless mana") {
        val d = GameTestDriver()
        d.registerCards(listOf(forest, GemstoneCaverns))
        d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val cavern = d.putCardInHand(me, "Gemstone Caverns")
        d.submit(PlayLand(me, cavern)).outcome shouldBe Outcome.Done
        d.state.getEntity(cavern)?.get<CountersComponent>()?.getCount(CounterType.LUCK) ?: 0 shouldBe 0
        val before = d.state
        d.submit(ActivateAbility(me, cavern, GemstoneCaverns.activatedAbilities[1].id,
            manaColorChoice = Color.BLUE)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.submit(ActivateAbility(me, cavern, GemstoneCaverns.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.state.getEntity(me)?.get<ManaPoolComponent>()?.colorless shouldBe 1
        d.isTapped(cavern) shouldBe true
    }
    for (color in Color.entries) {
        test("a luck counter enables $color mana and disables colorless mode") {
            val d = GameTestDriver()
            d.registerCards(listOf(forest, GemstoneCaverns))
            d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val cavern = d.putLandOnBattlefield(me, "Gemstone Caverns")
            d.replaceState(d.state.updateEntity(cavern) { it.with(CountersComponent().withAdded(CounterType.LUCK, 1)) })
            val before = d.state
            d.submit(ActivateAbility(me, cavern, GemstoneCaverns.activatedAbilities[0].id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe before
            d.submit(ActivateAbility(me, cavern, GemstoneCaverns.activatedAbilities[1].id,
                manaColorChoice = color)).outcome shouldBe Outcome.Done
            d.state.getEntity(me)?.get<ManaPoolComponent>()?.getAmount(color) shouldBe 1
            d.isTapped(cavern) shouldBe true
        }
    }
})

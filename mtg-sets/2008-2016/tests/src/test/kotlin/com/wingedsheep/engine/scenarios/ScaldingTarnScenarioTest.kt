package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.mtg.sets.definitions.zen.cards.ScaldingTarn
import com.wingedsheep.mtg.sets.definitions.leb.cards.VolcanicIsland

class ScaldingTarnScenarioTest : FunSpec({
    for (choice in listOf("Island", "Mountain", "Volcanic Island", "none")) {
        test("activation pays life and sacrifice, searches for $choice, and shuffles") {
            val d = GameTestDriver()
            d.registerCards(listOf(ScaldingTarn, VolcanicIsland) + listOf("Forest", "Island", "Mountain").map { CardDefinition.basicLand(it, Subtype(it)) })
            d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val island = d.putCardOnTopOfLibrary(me, "Island")
            val mountain = d.putCardOnTopOfLibrary(me, "Mountain")
            val dual = d.putCardOnTopOfLibrary(me, "Volcanic Island")
            val forest = d.putCardOnTopOfLibrary(me, "Forest")
            val tarn = d.putLandOnBattlefield(me, "Scalding Tarn")
            val ability = ScaldingTarn.script.activatedAbilities.single()
            d.submit(ActivateAbility(me, tarn, ability.id)).outcome shouldBe Outcome.Done
            d.getLifeTotal(me) shouldBe 19
            d.getGraveyard(me) shouldContain tarn
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContain island
            decision.options shouldContain mountain
            decision.options shouldContain dual
            decision.options shouldNotContain forest
            val beforeInvalid = d.state
            d.submit(SubmitDecision(me, CardsSelectedResponse(decision.id, listOf(forest))))
                .outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe beforeInvalid
            val chosen = when (choice) { "Island" -> listOf(island); "Mountain" -> listOf(mountain); "Volcanic Island" -> listOf(dual); else -> emptyList() }
            val shuffles = d.events.count { it is LibraryShuffledEvent }
            d.submitCardSelection(me, chosen)
            for (id in chosen) {
                d.state.getBattlefield() shouldContain id
                d.isTapped(id) shouldBe false
            }
            d.events.count { it is LibraryShuffledEvent } shouldBe shuffles + 1
        }
    }
})

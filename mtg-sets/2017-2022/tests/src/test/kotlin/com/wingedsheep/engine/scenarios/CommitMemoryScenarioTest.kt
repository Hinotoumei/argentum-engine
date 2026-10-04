package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent
import io.kotest.matchers.shouldBe

class CommitMemoryScenarioTest : ScenarioTestBase() {
    init {
        test("Commit // Memory has combined mana value ten and both spell types in hand") {
            val game = scenario().withPlayers().withCardInHand(1, "Commit // Memory").build()
            val id = game.findCardsInHand(1, "Commit // Memory").single()
            val card = game.state.getEntity(id)!!.get<CardComponent>()!!
            card.manaCost.cmc shouldBe 10
            card.typeLine.isInstant shouldBe true
            card.typeLine.isSorcery shouldBe true
        }
        test("an external exile permission offers Commit but cannot authorize Memory") {
            val game = scenario().withPlayers().withCardInExile(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 6).withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val id = game.state.getExile(game.player1Id).single()
            game.state = game.state.addMayPlayPermission(MayPlayPermission(
                id = EntityId.generate(), cardIds = setOf(id), controllerId = game.player1Id, timestamp = game.state.timestamp,
            ))
            val faces = game.getLegalActions(1).mapNotNull { it.action as? CastSpell }
                .filter { it.cardId == id }.map { it.faceIndex }.toSet()
            faces shouldBe setOf(0)
            game.execute(CastSpell(game.player1Id, id, faceIndex = 1)).error.isNullOrEmpty() shouldBe false
            game.state.getExile(game.player1Id).contains(id) shouldBe true
        }
        test("an external graveyard permission offers each face at its own cost") {
            val game = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 6).withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val id = game.findCardsInGraveyard(1, "Commit // Memory").single()
            game.state = game.state.addMayPlayPermission(MayPlayPermission(
                id = EntityId.generate(), cardIds = setOf(id), controllerId = game.player1Id, timestamp = game.state.timestamp,
            ))
            val faces = game.getLegalActions(1).mapNotNull { it.action as? CastSpell }
                .filter { it.cardId == id }.map { it.faceIndex }.toSet()
            faces shouldBe setOf(0, 1)
        }
        for (dualTarget in listOf(false, true)) {
            cardRegistry.register(card("Test Aftermath Bounce $dualTarget") {
                manaCost = "{1}{U}"
                typeLine = "Instant"
                spell {
                    val t = target(TargetSpellOrPermanent())
                    effect = if (dualTarget) Effects.ReturnSpellOrPermanentToOwnersHand(t)
                        else Effects.ReturnSpellToOwnersHand()
                }
            })
        }
        test("Commit moves an uncounterable spell into its owner's library") {
            val game = scenario().withPlayers().withCardInHand(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Carnage Tyrant").withLandsOnBattlefield(2, "Forest", 6)
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val tyrant = game.findCardsInHand(2, "Carnage Tyrant").single()
            val before = game.state.getLibrary(game.player2Id)
            game.execute(CastSpell(game.player2Id, tyrant)).error shouldBe null
            game.passPriority().error shouldBe null
            game.execute(CastSpell(game.player1Id,
                game.findCardsInHand(1, "Commit // Memory").single(),
                targets = listOf(ChosenTarget.Spell(tyrant)), faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.state.getLibrary(game.player2Id) shouldBe (before + tyrant)
            game.state.getGraveyard(game.player1Id).size shouldBe 1
        }
        for (lands in listOf(5, 6)) {
            test("Memory's graveyard action requires its six-mana face cost with $lands lands") {
                val game = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                    .withLandsOnBattlefield(1, "Island", lands)
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val memory = game.findCardsInGraveyard(1, "Commit // Memory").single()
                val offered = game.getLegalActions(1).filter {
                    val cast = it.action as? CastSpell
                    cast?.cardId == memory && cast.faceIndex == 1
                }
                offered.isNotEmpty() shouldBe (lands == 6)
                if (lands == 5) {
                    val before = game.state
                    (game.execute(CastSpell(game.player1Id, memory, faceIndex = 1)).error != null) shouldBe true
                    game.state shouldBe before
                }
            }
        }
        test("Memory cannot be cast during the opponent's main phase") {
            val game = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.passPriority().error shouldBe null
            val before = game.state
            val memory = game.findCardsInGraveyard(1, "Commit // Memory").single()
            (game.execute(CastSpell(game.player1Id, memory, faceIndex = 1)).error != null) shouldBe true
            game.state shouldBe before
        }
        test("Memory's seven draws respect Notion Thief") {
            var setup = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                .withCardOnBattlefield(1, "Notion Thief").withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(20) { setup = setup.withCardInLibrary(1, "Forest").withCardInLibrary(2, "Island") }
            val game = setup.build()
            game.execute(CastSpell(game.player1Id,
                game.findCardsInGraveyard(1, "Commit // Memory").single(), faceIndex = 1)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 14
            game.handSize(2) shouldBe 0
        }
        for (size in listOf(0, 1, 3)) {
            test("Commit puts a nonland permanent second from top of a library of size $size") {
                var setup = scenario().withPlayers().withCardInHand(1, "Commit // Memory")
                    .withLandsOnBattlefield(1, "Island", 4).withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(size) { setup = setup.withCardInLibrary(2, "Forest") }
                val game = setup.build()
                val target = game.findPermanent("Grizzly Bears")!!
                val before = game.state.getLibrary(game.player2Id)
                game.execute(CastSpell(game.player1Id,
                    game.findCardsInHand(1, "Commit // Memory").single(),
                    targets = listOf(ChosenTarget.Permanent(target)), faceIndex = 0)).error shouldBe null
                game.resolveStack()
                val expected = before.toMutableList().apply { add(minOf(1, size), target) }
                game.state.getLibrary(game.player2Id) shouldBe expected
            }
        }
        test("Commit rejects a land target without paying mana or moving the card") {
            val game = scenario().withPlayers().withCardInHand(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 4).withCardOnBattlefield(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val before = game.state
            val result = game.execute(CastSpell(game.player1Id,
                game.findCardsInHand(1, "Commit // Memory").single(),
                targets = listOf(ChosenTarget.Permanent(game.findPermanent("Forest")!!)), faceIndex = 0))
            (result.error != null) shouldBe true
            game.state shouldBe before
        }
        for (face in listOf(null, -1, 1, 2)) {
            test("hand cast rejects combined, invalid, or Aftermath face $face") {
                val game = scenario().withPlayers().withCardInHand(1, "Commit // Memory")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val before = game.state
                (game.execute(CastSpell(game.player1Id,
                    game.findCardsInHand(1, "Commit // Memory").single(), faceIndex = face)).error != null) shouldBe true
                game.state shouldBe before
            }
        }
        test("Memory resets both players' hands and graveyards, draws seven each, then exiles itself") {
            var setup = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                .withCardInGraveyard(1, "Shock").withCardInGraveyard(2, "Grizzly Bears")
                .withCardInHand(1, "Mountain").withCardInHand(2, "Swamp")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(12) { setup = setup.withCardInLibrary(1, "Forest").withCardInLibrary(2, "Island") }
            val game = setup.build()
            val memory = game.findCardsInGraveyard(1, "Commit // Memory").single()
            game.execute(CastSpell(game.player1Id, memory, faceIndex = 1)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 7
            game.handSize(2) shouldBe 7
            game.state.getGraveyard(game.player1Id).size shouldBe 0
            game.state.getGraveyard(game.player2Id).size shouldBe 0
            game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE)).contains(memory) shouldBe true
        }
        for (response in listOf("Counterspell", "Commit // Memory",
            "Test Aftermath Bounce false", "Test Aftermath Bounce true")) {
            test("Memory is exiled when an opponent responds with $response") {
                val game = scenario().withPlayers().withCardInGraveyard(1, "Commit // Memory")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInHand(2, response).withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val memory = game.findCardsInGraveyard(1, "Commit // Memory").single()
                game.execute(CastSpell(game.player1Id, memory, faceIndex = 1)).error shouldBe null
                game.passPriority().error shouldBe null
                game.execute(CastSpell(game.player2Id,
                    game.findCardsInHand(2, response).single(), targets = listOf(ChosenTarget.Spell(memory)),
                    faceIndex = if (response == "Commit // Memory") 0 else null)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 0
                game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE)).contains(memory) shouldBe true
            }
        }
    }
}

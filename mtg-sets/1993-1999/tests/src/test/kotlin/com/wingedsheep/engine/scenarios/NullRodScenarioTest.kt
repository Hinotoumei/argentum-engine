package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.wth.cards.NullRod
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class NullRodScenarioTest : FunSpec({
    val zoneArtifact = card("Zone ability artifact fixture") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            activateFromZone = Zone.GRAVEYARD
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
        activatedAbility {
            activateFromZone = Zone.HAND
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }
    val passiveArtifact = card("Passive artifact fixture") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifyStats(1, 1, filter = GroupFilter(GameObjectFilter.Creature.youControl()))
        }
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(2)
        }
    }
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(NullRod, zoneArtifact, passiveArtifact))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Mind Stone mana and draw activations reject atomically") {
        val d = setup()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Null Rod")
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        d.giveMana(me, Color.GREEN)
        for (ability in d.cardRegistry.getCard("Mind Stone")!!.script.activatedAbilities) {
            val before = d.state
            d.submit(ActivateAbility(me, stone, ability.id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe before
        }
        d.services.legalActionEnumerator.enumerate(d.state, me).none {
            val action = it.action
            action is ActivateAbility && action.sourceId == stone
        } shouldBe true
    }

    test("artifact static and triggered abilities continue to operate") {
        val d = setup()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Null Rod")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val artifact = d.putCardInHand(me, passiveArtifact.name)
        d.giveMana(me, Color.GREEN)
        d.castSpell(me, artifact).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(me) shouldBe 22
        // A third toughness from the artifact's static bonus lets this bear survive two damage.
        val shock = d.putCardInHand(me, "Shock")
        d.giveMana(me, Color.RED)
        d.castSpell(me, shock, listOf(bear)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getBattlefield().contains(bear) shouldBe true
    }

    test("automatic payment cannot tap Mind Stone while land mana remains available") {
        val d = setup()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Null Rod")
        d.putPermanentOnBattlefield(me, "Mind Stone")
        val spell = d.putCardInHand(me, "Mind Stone")
        val before = d.state
        d.castSpell(me, spell).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state shouldBe before
        d.putLandOnBattlefield(me, "Forest")
        d.putLandOnBattlefield(me, "Forest")
        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getBattlefield().contains(spell) shouldBe true
    }

    test("destroying Null Rod through Naturalize restores artifact mana") {
        val d = setup()
        val me = d.activePlayer!!
        val rod = d.putPermanentOnBattlefield(me, "Null Rod")
        val stone = d.putPermanentOnBattlefield(me, "Mind Stone")
        val naturalize = d.putCardInHand(me, "Naturalize")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, naturalize, listOf(rod)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(me).contains(rod) shouldBe true
        d.submit(ActivateAbility(me, stone, d.cardRegistry.getCard("Mind Stone")!!.script.activatedAbilities.first().id))
            .outcome shouldBe Outcome.Done
        d.isTapped(stone) shouldBe true
    }

    for (graveyard in listOf(false, true)) {
        test("artifact card activation outside battlefield is usable: graveyard $graveyard") {
            val d = setup()
            val me = d.activePlayer!!
            d.putPermanentOnBattlefield(me, "Null Rod")
            val artifact = if (graveyard) d.putCardInGraveyard(me, zoneArtifact.name)
                else d.putCardInHand(me, zoneArtifact.name)
            d.giveMana(me, Color.GREEN)
            val ability = zoneArtifact.script.activatedAbilities[if (graveyard) 0 else 1]
            d.submit(ActivateAbility(me, artifact, ability.id)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.getLifeTotal(me) shouldBe 21
        }
    }
})

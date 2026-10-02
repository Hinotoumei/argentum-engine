package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeathMarkerZoneTransitionTest : FunSpec({
    val mountain = basicLand("Mountain") { collectorNumber = "1" }
    val creature = card("Death Marker Creature") { typeLine = "Creature — Bear"; manaCost = "{0}"; power = 2; toughness = 2 }
    val artifact = card("Death Marker Artifact") { typeLine = "Artifact"; manaCost = "{0}" }
    val mark = card("Death Marker Spell") {
        typeLine = "Instant"; manaCost = "{0}"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.MarkExileOnDeath(t) }
    }
    fun setup() = GameTestDriver().also {
        it.registerCards(listOf(mountain, creature, artifact, mark))
        it.initMirrorMatch(Deck.of("Mountain" to 40)); it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (destination in listOf(Zone.GRAVEYARD, Zone.HAND)) test("death marker redirects only a graveyard movement; destination=$destination") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putPermanentOnBattlefield(me, creature.name)
        val spell = d.putCardInHand(me, mark.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        d.bothPass(); d.stackSize shouldBe 0
        d.state.floatingEffects.size shouldBe 1
        val result = d.zones.moveToZone(d.state, target, destination)
        result.state.getZone(com.wingedsheep.engine.state.ZoneKey(me,
            if (destination == Zone.GRAVEYARD) Zone.EXILE else Zone.HAND)).contains(target) shouldBe true
        result.state.floatingEffects.size shouldBe 0
    }
    test("a noncreature permanent does not acquire a creature death marker") {
        val d = setup(); val me = d.activePlayer!!
        val target = d.putPermanentOnBattlefield(me, artifact.name)
        val spell = d.putCardInHand(me, mark.name)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        d.bothPass(); d.stackSize shouldBe 0
        d.state.floatingEffects.size shouldBe 0
    }
})

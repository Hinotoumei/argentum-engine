package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class IntrinsicCounterProtectionTest : FunSpec({
    val mountain = basicLand("Mountain") { collectorNumber = "1" }
    val threshold = card("Intrinsic X Protection") {
        manaCost = "{X}"; typeLine = "Sorcery"
        staticAbility { ability = ConditionalStaticAbility(CompositeStaticAbility(listOf(CantBeCountered)),
            Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 5)) }
        spell { effect = Effects.GainLife(1) }
    }
    val granter = card("Stack Grant Witness") {
        manaCost = "{0}"; typeLine = "Creature — Sliver"; power = 1; toughness = 1
        staticAbility { ability = GrantCantBeCountered(GameObjectFilter.Any.withSubtype("Sliver")) }
    }
    val sliver = card("Stack Grant Recipient") {
        manaCost = "{0}"; typeLine = "Creature — Sliver"; power = 1; toughness = 1
    }
    fun setup() = GameTestDriver().also {
        it.registerCards(listOf(mountain, threshold, granter, sliver))
        it.initMirrorMatch(Deck.of("Mountain" to 40)); it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    for (saved in listOf(false, true)) for (x in listOf(4, 5)) test("intrinsic X threshold uses current saved spell; X=$x saved=$saved") {
        val d = setup(); val me = d.activePlayer!!
        val spell = d.putCardInHand(me, threshold.name); d.giveColorlessMana(me, x)
        d.castXSpell(me, spell, x).error shouldBe null
        if (saved) d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        val result = d.services.spellCounterer.counterSpell(d.state, spell)
        result.error shouldBe null; result.state.stack.contains(spell) shouldBe (x >= 5)
    }
    test("a battlefield grant does not protect its source while that source is a spell") {
        val d = setup(); val me = d.activePlayer!!
        val spell = d.putCardInHand(me, granter.name); d.castSpell(me, spell).error shouldBe null
        val result = d.services.spellCounterer.counterSpell(d.state, spell)
        result.error shouldBe null; result.state.stack.contains(spell) shouldBe false
    }
    test("a battlefield grant still protects other matching spells after entry") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, granter.name)
        val spell = d.putCardInHand(me, sliver.name); d.castSpell(me, spell).error shouldBe null
        val result = d.services.spellCounterer.counterSpell(d.state, spell)
        result.error shouldBe null; result.state.stack.contains(spell) shouldBe true
    }
})

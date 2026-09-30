import unittest

from inuyasha_engine import (
    AttachmentState,
    CharacterState,
    Color,
    GameState,
    JewelShard,
    PlayerState,
    RuleError,
    TimingWindow,
)


def char(title, controller, red, blue=0, green=0, purple=0, black=0):
    return CharacterState(
        title=title,
        controller_id=controller,
        color_values={
            Color.RED: red,
            Color.BLUE: blue,
            Color.GREEN: green,
            Color.PURPLE: purple,
            Color.BLACK: black,
        },
    )


class AttackSequenceTests(unittest.TestCase):
    def make_game(self, attacker_red=5, defender_red=4):
        p1 = PlayerState("P1", [JewelShard("P1-S1"), JewelShard("P1-S2")])
        p2 = PlayerState("P2", [JewelShard("P2-S1"), JewelShard("P2-S2")])
        attacker = char("TEST Attacker", "P1", attacker_red)
        defender = char("TEST Defender", "P2", defender_red)
        game = GameState(
            players={"P1": p1, "P2": p2},
            turn_active_player_id="P2",  # proves attack active player is contextual
            characters=[attacker, defender],
        )
        return game, attacker, defender

    def test_successful_attack_defeats_and_steals_one_shard(self):
        game, attacker, defender = self.make_game(5, 4)
        before_p1 = len(game.player("P1").jewel_shards)
        before_p2 = len(game.player("P2").jewel_shards)

        ctx = game.attack(attacker, defender, Color.RED)

        self.assertTrue(attacker.expended)
        self.assertTrue(defender.defeated)
        self.assertFalse(defender.face_up)
        self.assertTrue(defender.expended)
        self.assertTrue(ctx.defender_defeated_by_attack)
        self.assertTrue(ctx.shard_stolen)
        self.assertEqual(len(game.player("P1").jewel_shards), before_p1 + 1)
        self.assertEqual(len(game.player("P2").jewel_shards), before_p2 - 1)

    def test_tie_defeats_defender(self):
        game, attacker, defender = self.make_game(4, 4)
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertTrue(ctx.defender_defeated_by_attack)
        self.assertTrue(defender.defeated)

    def test_lower_attack_value_does_not_defeat_or_steal(self):
        game, attacker, defender = self.make_game(3, 4)
        before = [len(game.player("P1").jewel_shards), len(game.player("P2").jewel_shards)]
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertFalse(ctx.defender_defeated_by_attack)
        self.assertFalse(ctx.shard_stolen)
        self.assertFalse(defender.defeated)
        self.assertEqual(
            [len(game.player("P1").jewel_shards), len(game.player("P2").jewel_shards)],
            before,
        )

    def test_attached_cards_turn_facedown_and_expended_with_defeated_character(self):
        game, attacker, defender = self.make_game(8, 1)
        item = AttachmentState("TEST Attached Item")
        defender.attachments.append(item)
        game.attack(attacker, defender, Color.RED)
        self.assertFalse(item.face_up)
        self.assertTrue(item.expended)

    def test_attack_windows_are_in_ard_order_on_success(self):
        game, attacker, defender = self.make_game(5, 4)
        game.attack(attacker, defender, Color.RED)
        self.assertEqual(
            [e.window for e in game.event_log],
            [
                TimingWindow.CHARACTER_EXPENDED,
                TimingWindow.WHEN_ATTACKING,
                TimingWindow.WHEN_ATTACKED,
                TimingWindow.COMPARING_COLOR_VALUES,
                TimingWindow.CHARACTER_DEFEATED,
                TimingWindow.JEWEL_SHARD_STOLEN,
                TimingWindow.ATTACK_END,
            ],
        )

    def test_attack_windows_skip_defeat_and_shard_windows_on_failure(self):
        game, attacker, defender = self.make_game(1, 9)
        game.attack(attacker, defender, Color.RED)
        self.assertEqual(
            [e.window for e in game.event_log],
            [
                TimingWindow.CHARACTER_EXPENDED,
                TimingWindow.WHEN_ATTACKING,
                TimingWindow.WHEN_ATTACKED,
                TimingWindow.COMPARING_COLOR_VALUES,
                TimingWindow.ATTACK_END,
            ],
        )

    def test_attacking_player_is_attack_active_even_when_not_turn_active(self):
        game, attacker, defender = self.make_game(5, 4)
        self.assertEqual(game.turn_active_player_id, "P2")
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertEqual(ctx.attacking_player_id, "P1")
        self.assertEqual(ctx.turn_active_player_id, "P2")
        self.assertTrue(all(e.active_player_id == "P1" for e in game.event_log))
        self.assertEqual(game.turn_active_player_id, "P2")

    def test_final_color_value_includes_temporary_and_constant_modifiers(self):
        game, attacker, defender = self.make_game(2, 5)
        attacker.temporary_color_modifiers[Color.RED] = 2
        attacker.constant_color_modifiers[Color.RED] = 2
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertEqual(ctx.attacker_final_value, 6)
        self.assertEqual(ctx.defender_final_value, 5)
        self.assertTrue(defender.defeated)

    def test_window_hook_can_change_value_before_comparison(self):
        game, attacker, defender = self.make_game(2, 4)

        def when_attacking_bonus(state, ctx, event):
            ctx.attacker.temporary_color_modifiers[ctx.color] = 3

        game.register_window_handler(TimingWindow.WHEN_ATTACKING, when_attacking_bonus)
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertEqual(ctx.attacker_final_value, 5)
        self.assertTrue(defender.defeated)

    def test_expended_character_cannot_attack(self):
        game, attacker, defender = self.make_game(5, 4)
        attacker.ready = False
        with self.assertRaises(RuleError):
            game.attack(attacker, defender, Color.RED)

    def test_no_shard_window_if_victim_has_no_shards(self):
        game, attacker, defender = self.make_game(5, 4)
        game.player("P2").jewel_shards.clear()
        ctx = game.attack(attacker, defender, Color.RED)
        self.assertTrue(defender.defeated)
        self.assertFalse(ctx.shard_stolen)
        self.assertNotIn(TimingWindow.JEWEL_SHARD_STOLEN, [e.window for e in game.event_log])


if __name__ == "__main__":
    unittest.main()

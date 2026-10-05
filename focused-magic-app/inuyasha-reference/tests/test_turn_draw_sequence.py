import unittest

from inuyasha_engine import GameState, PlayerState, RuleError, TimingWindow, TurnWindowEvent


def deck(prefix, count=10):
    return [f"{prefix}-{i}" for i in range(1, count + 1)]


class TurnDrawSequenceTests(unittest.TestCase):
    def make_game(self):
        p1 = PlayerState("P1", draw_pile=deck("P1"))
        p2 = PlayerState("P2", draw_pile=deck("P2"))
        return GameState(players={"P1": p1, "P2": p2}, turn_active_player_id="P1")

    def test_turn_start_and_draw_windows_follow_ard_order(self):
        game = self.make_game()
        game.start_turn_and_draw()
        self.assertEqual(
            [event.window for event in game.event_log],
            [
                TimingWindow.TURN_START,
                TimingWindow.DRAW_START,
                TimingWindow.CARDS_DRAWN,
                TimingWindow.CARDS_DRAWN,
                TimingWindow.DRAW_END,
            ],
        )

    def test_active_player_draws_three_then_opponent_draws_three(self):
        game = self.make_game()
        ctx = game.start_turn_and_draw()

        self.assertEqual(ctx.active_drawn_cards, ["P1-1", "P1-2", "P1-3"])
        self.assertEqual(ctx.opposing_drawn_cards, ["P2-1", "P2-2", "P2-3"])
        self.assertEqual(game.player("P1").hand, ["P1-1", "P1-2", "P1-3"])
        self.assertEqual(game.player("P2").hand, ["P2-1", "P2-2", "P2-3"])
        self.assertEqual(game.player("P1").draw_pile[:2], ["P1-4", "P1-5"])
        self.assertEqual(game.player("P2").draw_pile[:2], ["P2-4", "P2-5"])
        self.assertTrue(ctx.draw_step_complete)

    def test_draw_events_record_who_drew_without_changing_turn_active_player(self):
        game = self.make_game()
        game.start_turn_and_draw()
        draw_events = [event for event in game.event_log if event.window == TimingWindow.CARDS_DRAWN]

        self.assertEqual([event.subject_player_id for event in draw_events], ["P1", "P2"])
        self.assertTrue(all(event.active_player_id == "P1" for event in draw_events))
        self.assertEqual(game.turn_active_player_id, "P1")

    def test_turn_window_handler_sees_exact_draw_order(self):
        game = self.make_game()
        observed = []

        def observe(state, ctx, event):
            self.assertIsInstance(event, TurnWindowEvent)
            observed.append((event.window, event.subject_player_id))

        game.register_window_handler(TimingWindow.TURN_START, observe)
        game.register_window_handler(TimingWindow.DRAW_START, observe)
        game.register_window_handler(TimingWindow.CARDS_DRAWN, observe)
        game.register_window_handler(TimingWindow.DRAW_END, observe)
        game.start_turn_and_draw()

        self.assertEqual(
            observed,
            [
                (TimingWindow.TURN_START, None),
                (TimingWindow.DRAW_START, None),
                (TimingWindow.CARDS_DRAWN, "P1"),
                (TimingWindow.CARDS_DRAWN, "P2"),
                (TimingWindow.DRAW_END, None),
            ],
        )

    def test_insufficient_active_deck_fails_closed_before_any_mutation(self):
        p1 = PlayerState("P1", draw_pile=["A", "B"])
        p2 = PlayerState("P2", draw_pile=deck("P2"))
        game = GameState(players={"P1": p1, "P2": p2}, turn_active_player_id="P1")

        with self.assertRaises(RuleError):
            game.start_turn_and_draw()

        self.assertEqual(p1.draw_pile, ["A", "B"])
        self.assertEqual(p1.hand, [])
        self.assertEqual(p2.hand, [])
        self.assertEqual(game.event_log, [])

    def test_insufficient_opponent_deck_fails_closed_before_active_player_draws(self):
        p1 = PlayerState("P1", draw_pile=deck("P1"))
        p2 = PlayerState("P2", draw_pile=["A", "B"])
        game = GameState(players={"P1": p1, "P2": p2}, turn_active_player_id="P1")

        with self.assertRaises(RuleError):
            game.start_turn_and_draw()

        self.assertEqual(p1.hand, [])
        self.assertEqual(p1.draw_pile, deck("P1"))
        self.assertEqual(p2.hand, [])
        self.assertEqual(game.event_log, [])

    def test_turn_draw_engine_requires_exactly_two_players(self):
        game = GameState(
            players={
                "P1": PlayerState("P1", draw_pile=deck("P1")),
                "P2": PlayerState("P2", draw_pile=deck("P2")),
                "P3": PlayerState("P3", draw_pile=deck("P3")),
            },
            turn_active_player_id="P1",
        )
        with self.assertRaises(RuleError):
            game.start_turn_and_draw()


if __name__ == "__main__":
    unittest.main()
